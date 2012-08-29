/**
 * Copyright 2012 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stackmob.sdk.api;

import com.stackmob.sdk.api.StackMob.OAuthVersion;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;

import java.util.Map;

/**
 * Sets defaults for newly create StackMob objects. This class is meant to be filled in when using the SDK source directly, and isn't useful when working
 * with the SDK as a jar. For Android, use StackMobCommon.init instead.
 */
public class StackMobConfiguration {

    public static final OAuthVersion OAUTH_VERSION = OAuthVersion.Two;

    public static final String DEFAULT_API_KEY = "DEFAULT_API_KEY";//do not change this
    public static final String DEFAULT_API_SECRET = "DEFAULT_API_SECRET";//do not change this

    public static final String API_KEY = DEFAULT_API_KEY;
    public static final String API_SECRET = DEFAULT_API_SECRET; // Only change this for OAuth1
    public static String USER_OBJECT_NAME = "user";
    public static Integer API_VERSION = 0;

    public static String API_URL_FORMAT = "api.mob1.stackmob.com";
    public static String PUSH_API_URL_FORMAT = "push.mob1.stackmob.com";

    public static boolean ENABLE_LOGGING = false;

    private static StackMobRedirectedCallback redirectedCallback = new StackMobRedirectedCallback() {
        @Override public void redirected(String originalURL, Map<String, String> redirectHeaders, String redirectBody, String newURL) {
            //do nothing for now
        }
    };
    
    static StackMob newStackMob() {
        StackMob stackmob = new StackMob(OAUTH_VERSION,
                                         API_KEY,
                                         API_SECRET,
                                         USER_OBJECT_NAME,
                                         null,
                                         API_VERSION,
                                         API_URL_FORMAT,
                                         PUSH_API_URL_FORMAT,
                                         redirectedCallback);
        if(OAUTH_VERSION == OAuthVersion.Two && !DEFAULT_API_SECRET.equals(API_SECRET)) {
            throw new IllegalStateException("The private key isn't necessary for oauth2. Leave it as " + DEFAULT_API_SECRET);
        }
        StackMob.getLogger().setLogging(ENABLE_LOGGING);
        StackMob.getLogger().logDebug("Starting java sdk version %s running on %s", StackMob.getVersion(), System.getProperty("os.name"));
        return stackmob;
    }

}
