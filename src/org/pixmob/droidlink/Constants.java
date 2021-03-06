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
package org.pixmob.droidlink;

/**
 * Application constants.
 * @author Pixmob
 */
public final class Constants {
    /**
     * Server host.
     */
    public static final String SERVER_HOST = "mydroidlink.appspot.com";
    /**
     * Application name.
     */
    public static final String APPLICATION_NAME = "Droid Link";
    /**
     * Remote API version.
     */
    public static final int REMOTE_API_VERSION = 1;
    
    /**
     * Set to <code>true</code> to enable development mode. When a release is
     * being built, make sure this flag is set to <code>false</code>.
     */
    public static final boolean DEVELOPER_MODE = true;
    
    /**
     * Use this tag for every logging statements.
     */
    public static final String TAG = "DroidLink";
    
    public static final String SHARED_PREFERENCES_FILE = "sharedprefs";
    public static final String SP_KEY_ACCOUNT = "account";
    public static final String SP_KEY_IGNORE_RECEIVED_SMS = "ignoreReceivedSms";
    public static final String SP_KEY_IGNORE_MISSED_CALLS = "ignoreMissedCalls";
    public static final String SP_KEY_DEVICE_ID = "deviceId";
    public static final String SP_KEY_DEVICE_NAME = "deviceName";
    public static final String SP_KEY_DEVICE_C2DM = "deviceC2dm";
    public static final String SP_KEY_EVENT_LIST_VISIBLE = "eventListVisible";
    public static final String SP_KEY_UNREAD_EVENT_COUNT = "unreadEventCount";
    public static final String SP_KEY_EVENT_MAX_AGE = "eventMaxAge";
    
    public static final String C2DM_SENDER_ID = "pixmobstudio@gmail.com";
    public static final String C2DM_MESSAGE_EXTRA = "message";
    public static final String C2DM_MESSAGE_SYNC = "sync";
    public static final String C2DM_ACCOUNT_EXTRA = "account";
    public static final String C2DM_SYNC_EXTRA = "token";
    
    public static final String EXTRA_FORCE_UPLOAD = "upload";
    
    public static final String ACTION_SYNC = "org.pixmob.droidlink.sync";
    public static final String EXTRA_RUNNING = "running";
    
    public static final String ACTION_NEW_EVENT = "org.pixmob.droidlink.newevent";
    public static final String EXTRA_EVENT_COUNT = "eventCount";
    public static final String EXTRA_EVENT_ID = "eventId";
    
    public static final int NEW_EVENT_NOTIFICATION = R.string.received_new_event;
    
    /**
     * Google account type.
     */
    public static final String GOOGLE_ACCOUNT = "com.google";
    
    /**
     * When opening the application, if the last sync time is over this limit, a
     * synchronization is started.
     */
    public static final long MAX_SYNC_AGE = 1000 * 60 * 30;
    
    private Constants() {
    }
}
