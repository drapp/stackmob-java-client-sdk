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
package com.stackmob.sdk.push;

import com.stackmob.sdk.api.StackMobSession;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.net.HttpVerbWithoutPayload;
import com.stackmob.sdk.request.StackMobPushRequest;
import com.stackmob.sdk.request.StackMobRequest;
import com.stackmob.sdk.request.StackMobRequestWithoutPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class StackMobPush {

    private class RegistrationIDAndUser {

        public String userId;
        public Map<String, String> token = new HashMap<String, String>();
        public Boolean overwrite = null;

        public RegistrationIDAndUser(String registrationID, String user) {
            this(registrationID, user, StackMobPushToken.TokenType.Android);
        }

        public RegistrationIDAndUser(String registrationID, String user, StackMobPushToken.TokenType platform) {
            userId = user;
            token.put("token", registrationID);
            token.put("type", platform.toString());
        }
        public RegistrationIDAndUser(String registrationID, String user, StackMobPushToken.TokenType platform, boolean overwrite) {
            this(registrationID, user, platform);
            this.overwrite = overwrite;
        }

        public RegistrationIDAndUser(String registrationID, String user, boolean overwrite) {
            this(registrationID, user, defaultPushType, overwrite);
        }
    }


    private StackMobPushToken.TokenType defaultPushType = StackMobPushToken.TokenType.Android;

    /**
     * Sets the type of push this StackMob instance will do. The default is
     * GCM. Use this to switch back to C2DM if you need to
     * @param type C2DM or GCM
     */
    public void setPushType(StackMobPushToken.TokenType type) {
        this.defaultPushType = type;
    }

    private ExecutorService executor;
    private StackMobSession session;
    private String host;
    private StackMobRedirectedCallback redirectedCallback;

    public StackMobPush(ExecutorService executor, StackMobSession session, String host, StackMobRedirectedCallback redirectedCallback) {
        this.executor = executor;
        this.session = session;
        this.host = host;
        this.redirectedCallback = redirectedCallback;
    }

    ////////////////////
    //Push Notifications
    ////////////////////

    /**
     * send a push notification to a group of tokens
     * @param payload the payload of the push notification to send
     * @param tokens the tokens to which to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void pushToTokens(Map<String, String> payload,
                                                  List<StackMobPushToken> tokens,
                                                  StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        Map<String, Object> payloadMap = new HashMap<String, Object>();

        payloadMap.put("kvPairs", payload);
        finalPayload.put("payload", payloadMap);
        finalPayload.put("tokens", tokens);

        postPush("push_tokens_universal", finalPayload, callback);
    }

    /**
     * send a push notification to a group of users.
     * @param payload the payload to send
     * @param userIds the IDs of the users to which to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void pushToUsers(Map<String, String> payload,
                                                 List<String> userIds,
                                                 StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        finalPayload.put("kvPairs", payload);
        finalPayload.put("userIds", userIds);
        postPush("push_users_universal", finalPayload, callback);
    }

    /**
     * register a user for Android Push notifications. This uses GCM unless specified otherwise.
     * @param username the StackMob username to associate with this token
     * @param registrationID the GCM registration ID obtained from GCM see http://developer.android.com/guide/google/gcm/gcm.html#registering for detail on how to get this ID
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void registerForPushWithUser(String username,
                                                             String registrationID,
                                                             StackMobRawCallback callback) {
        registerForPushWithUser(username, registrationID, false, callback);
    }

    /**
     * register a user for Android Push notifications. This uses GCM unless specified otherwise.
     * @param username the StackMob username to associate with this token
     * @param registrationID the GCM registration ID obtained from GCM see http://developer.android.com/guide/google/gcm/gcm.html#registering for detail on how to get this ID
     * @param overwrite whether to overwrite existing entries
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void registerForPushWithUser(String username,
                                                             String registrationID,
                                                             boolean overwrite,
                                                             StackMobRawCallback callback) {
        RegistrationIDAndUser tokenAndUser = new RegistrationIDAndUser(registrationID, username, defaultPushType, overwrite);
        postPush("register_device_token_universal", tokenAndUser, callback);
    }

    /**
     * register a user for Android Push notifications.
     * @param username the StackMob username to associate with this token
     * @param token a token containing a registration id and platform
     * @param overwrite whether to overwrite existing entries
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void registerForPushWithUser(String username,
                                                             StackMobPushToken token,
                                                             boolean overwrite,
                                                             StackMobRawCallback callback) {
        RegistrationIDAndUser tokenAndUser = new RegistrationIDAndUser(token.getToken(), username, token.getTokenType(), overwrite);
        postPush("register_device_token_universal", tokenAndUser, callback);
    }


    /**
     * get all the tokens for the each of the given users
     * @param usernames the users whose tokens to get
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void getTokensForUsers(List<String> usernames,
                                                       StackMobRawCallback callback) {
        final StringBuilder userIds = new StringBuilder();
        boolean first = true;
        for(String username : usernames) {
            if(!first) {
                userIds.append(",");
            }
            first = false;
            userIds.append(username);
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put("userIds", userIds.toString());
        getPush("get_tokens_for_users_universal", params, callback);
    }

    /**
     * broadcast a push notification to all users of this app. use this method sparingly, especially if you have a large app
     * @param payload the payload to broadcast
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void broadcastPushNotification(Map<String, String> payload,
                                                               StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        finalPayload.put("kvPairs", payload);
        postPush("push_broadcast", finalPayload, callback);
    }

    /**
     * remove a push token for this app
     * @param token the token
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void removePushToken(StackMobPushToken token, StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        finalPayload.put("token", token.getToken());
        finalPayload.put("type", token.getTokenType().toString());
        postPush("remove_token_universal", finalPayload, callback);
    }


    private void postPush(String path, Object requestObject, StackMobRawCallback callback) {
        new StackMobPushRequest(this.executor,
                this.session,
                requestObject,
                path,
                callback,
                this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }
    /**
     * do a GET request to the stackmob push service
     * @param path the path of the push request
     * @param arguments the arguments to pass to the push service, in the query string
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * contains no information about the response - that will be passed to the callback when the response comes back
     */
    private void getPush(String path, Map<String, String> arguments, StackMobRawCallback callback) {
        new StackMobRequestWithoutPayload(this.executor,
                this.session,
                HttpVerbWithoutPayload.GET,
                StackMobRequest.EmptyHeaders,
                arguments,
                path,
                callback,
                this.redirectedCallback).setUrlFormat(this.host).sendRequest();
    }

}
