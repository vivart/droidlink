/*
 * Copyright (C) 2011 Pixmob (http://github.com/pixmob)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pixmob.droidlink.net;

import static org.pixmob.droidlink.Constants.APPLICATION_NAME;
import static org.pixmob.droidlink.Constants.DEVELOPER_MODE;
import static org.pixmob.droidlink.Constants.REMOTE_API_VERSION;
import static org.pixmob.droidlink.Constants.SERVER_HOST;
import static org.pixmob.droidlink.Constants.SHARED_PREFERENCES_FILE;
import static org.pixmob.droidlink.Constants.SP_KEY_ACCOUNT;
import static org.pixmob.droidlink.Constants.SP_KEY_DEVICE_ID;
import static org.pixmob.droidlink.Constants.TAG;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pixmob.appengine.client.AppEngineAuthenticationException;
import org.pixmob.appengine.client.AppEngineClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

/**
 * Network client for sending requests to the remote server. Requests are made
 * using the REST pattern, where data is encoded with JSON.
 * @author Pixmob
 */
public class NetworkClient {
    private static final String CHARSET = "UTF-8";
    private static String applicationVersion;
    
    private final AppEngineClient client;
    private final String deviceId;
    private final String account;
    
    private NetworkClient(final AppEngineClient client, final String accountName,
            final String deviceId) {
        this.client = client;
        this.account = accountName;
        this.deviceId = deviceId;
    }
    
    public static NetworkClient newInstance(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_FILE,
            Context.MODE_PRIVATE);
        
        // An account name is required for sending authenticated requests.
        final String account = prefs.getString(SP_KEY_ACCOUNT, null);
        if (account == null) {
            Log.w(TAG, "No account set for this device");
            return null;
        }
        
        // A device identifier is nearly required for every requests.
        final String deviceId = prefs.getString(SP_KEY_DEVICE_ID, null);
        if (deviceId == null) {
            Log.w(TAG, "No device identifier set");
            return null;
        }
        
        final AppEngineClient gaeClient = new AppEngineClient(context, SERVER_HOST);
        gaeClient.setAccount(account);
        gaeClient.setHttpUserAgent(generateUserAgent(context));
        
        return new NetworkClient(gaeClient, account, deviceId);
    }
    
    private static final String generateUserAgent(Context context) {
        if (applicationVersion == null) {
            try {
                applicationVersion = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionName;
            } catch (NameNotFoundException e) {
                applicationVersion = "0.0.0";
            }
        }
        return APPLICATION_NAME + "/" + applicationVersion + " (" + Build.MANUFACTURER + " "
                + Build.MODEL + " with Android " + Build.VERSION.RELEASE + "/"
                + Build.VERSION.SDK_INT + ")";
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getAccount() {
        return account;
    }
    
    public JSONObject get(String serviceUri) throws IOException, AppEngineAuthenticationException {
        return (JSONObject) execute(HttpMethod.GET, serviceUri, null);
    }
    
    public JSONArray getAsArray(String serviceUri) throws IOException,
            AppEngineAuthenticationException {
        return (JSONArray) execute(HttpMethod.GET, serviceUri, null);
    }
    
    public JSONObject put(String serviceUri, JSONObject data) throws IOException,
            AppEngineAuthenticationException {
        return (JSONObject) execute(HttpMethod.PUT, serviceUri, data);
    }
    
    public JSONObject post(String serviceUri, JSONObject data) throws IOException,
            AppEngineAuthenticationException {
        return (JSONObject) execute(HttpMethod.POST, serviceUri, data);
    }
    
    public void delete(String serviceUri) throws IOException, AppEngineAuthenticationException {
        execute(HttpMethod.DELETE, serviceUri, null);
    }
    
    public void close() {
        client.close();
    }
    
    public HttpResponse execute(HttpUriRequest request) throws IOException,
            AppEngineAuthenticationException {
        HttpResponse resp = null;
        try {
            resp = client.execute(request);
            
            final int statusCode = resp.getStatusLine().getStatusCode();
            if (DEVELOPER_MODE) {
                Log.i(TAG, "Result for request " + request.getURI().toASCIIString() + ": "
                        + statusCode);
            }
            
            return resp;
        } catch (AppEngineAuthenticationException e) {
            closeResources(request, resp);
            throw e;
        } catch (IOException e) {
            closeResources(request, resp);
            throw e;
        }
    }
    
    private Object execute(HttpMethod httpMethod, String serviceUri, JSONObject data)
            throws IOException, AppEngineAuthenticationException {
        final String requestUri = createServiceUri(serviceUri);
        final HttpUriRequest request = httpMethod.createRequest(requestUri, data);
        prepareJsonRequest(request);
        
        if (DEVELOPER_MODE) {
            Log.i(TAG, "Sending request to remote server: " + requestUri);
            if (request instanceof HttpEntityEnclosingRequestBase) {
                final HttpEntityEnclosingRequestBase r = (HttpEntityEnclosingRequestBase) request;
                final HttpEntity body = r.getEntity();
                if (body != null) {
                    final String strBody = EntityUtils.toString(body, CHARSET);
                    Log.d(TAG, "Body for request " + requestUri + ": " + strBody);
                }
            }
        }
        
        final HttpResponse resp = execute(request);
        final int statusCode = resp.getStatusLine().getStatusCode();
        if (isStatusNotFound(statusCode)) {
            throw new NetworkClientException(requestUri, statusCode, "Resource not found");
        }
        if (isStatusError(statusCode)) {
            throw new NetworkClientException(requestUri, statusCode,
                    "Request failed on remote server");
        }
        if (!isStatusOK(statusCode)) {
            throw new NetworkClientException(requestUri, statusCode, "Request failed with error "
                    + statusCode);
        }
        
        final HttpEntity entity = resp.getEntity();
        if (entity == null) {
            if (DEVELOPER_MODE) {
                Log.d(TAG, "No JSON result for request " + requestUri);
            }
            return null;
        }
        
        final String strResp = EntityUtils.toString(entity, CHARSET);
        if (TextUtils.isEmpty(strResp)) {
            if (DEVELOPER_MODE) {
                Log.d(TAG, "Empty JSON result for request " + requestUri);
            }
            return null;
        }
        
        if (DEVELOPER_MODE) {
            Log.d(TAG, "JSON result for request " + requestUri + ": " + strResp);
        }
        try {
            if (strResp.startsWith("[")) {
                // This is a JSON array.
                return new JSONArray(strResp);
            }
            return new JSONObject(strResp);
        } catch (JSONException e) {
            throw new NetworkClientException(requestUri, "Invalid JSON result", e);
        }
    }
    
    private static void closeResources(HttpUriRequest request, HttpResponse response) {
        try {
            request.abort();
        } catch (UnsupportedOperationException ignore) {
        }
        if (response != null) {
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                try {
                    entity.consumeContent();
                } catch (IOException ignore) {
                }
            }
        }
    }
    
    /**
     * Create a service Uri.
     */
    private static String createServiceUri(String resource) {
        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }
        return "https://" + SERVER_HOST + "/api/" + REMOTE_API_VERSION + resource;
    }
    
    /**
     * Check if the status code is a valid response.
     */
    private static boolean isStatusOK(int statusCode) {
        return statusCode == 200 || statusCode == 204 || statusCode == 201;
    }
    
    /**
     * Check if the status code is an error indicating that a resource was not
     * found.
     */
    private static boolean isStatusNotFound(int statusCode) {
        return statusCode == 404;
    }
    
    /**
     * Check if the status code is an error indicating the server failed to
     * process the request.
     */
    private static boolean isStatusError(int statusCode) {
        return statusCode == 500;
    }
    
    /**
     * Prepare a request for sending a Json formatted query.
     */
    private static void prepareJsonRequest(HttpRequest req) {
        req.setHeader(HTTP.CONTENT_TYPE, "application/json");
        req.addHeader("Accept", "application/json");
    }
    
    private static enum HttpMethod {
        GET, PUT, POST, DELETE;
        
        public HttpUriRequest createRequest(String requestUri, JSONObject data)
                throws UnsupportedEncodingException {
            switch (this) {
                case GET:
                    return new HttpGet(requestUri);
                case POST:
                    final HttpPost post = new HttpPost(requestUri);
                    if (data != null) {
                        post.setEntity(new StringEntity(data.toString(), CHARSET));
                    }
                    return post;
                case PUT:
                    final HttpPut put = new HttpPut(requestUri);
                    if (data != null) {
                        put.setEntity(new StringEntity(data.toString(), CHARSET));
                    }
                    return put;
                case DELETE:
                    return new HttpDelete(requestUri);
                default:
                    // Unlikely to happen.
                    throw new IllegalStateException("Unsupported Http method: " + this);
            }
        }
    }
}
