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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.net.HttpVerb;
import com.stackmob.sdk.net.HttpVerbWithPayload;
import com.stackmob.sdk.net.HttpVerbWithoutPayload;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class StackMobAccessTokenRequest extends StackMobRequest {

    Map<String, String> bodyParams;

    public StackMobAccessTokenRequest(ExecutorService executor,
                                      StackMobSession session,
                                      String method,
                                      Map<String, String> params,
                                      StackMobRawCallback cb,
                                      StackMobRedirectedCallback redirCb) {
        super(executor, session, HttpVerbWithPayload.POST, StackMobRequest.EmptyHeaders, StackMobRequest.EmptyParams, method, getIntermediaryCallback(session, cb), redirCb);
        bodyParams = params;
        isSecure = true;
    }

    private static StackMobRawCallback getIntermediaryCallback(final StackMobSession session, final StackMobRawCallback callback) {
        return new StackMobRawCallback() {
            @Override
            public void done(HttpVerb requestVerb, String requestURL, List<Map.Entry<String, String>> requestHeaders, String requestBody, Integer responseStatusCode, List<Map.Entry<String, String>> responseHeaders, byte[] responseBody) {
                JsonElement responseElt = new JsonParser().parse(new String(responseBody));
                byte[] finalResponseBody = responseBody;
                if(responseElt.isJsonObject()) {
                    // Parse out the token and expiration
                    JsonElement tokenElt = responseElt.getAsJsonObject().get("access_token");
                    if(tokenElt != null && tokenElt.isJsonPrimitive() && tokenElt.getAsJsonPrimitive().isString()) {
                        session.setOAuth2Token(tokenElt.getAsString());
                    }
                    JsonElement expirationElt = responseElt.getAsJsonObject().get("expires_in");
                    if(expirationElt != null && expirationElt.isJsonPrimitive() && expirationElt.getAsJsonPrimitive().isNumber()) {
                        session.setOAuth2TokenExpiration(expirationElt.getAsInt());
                    }
                    JsonElement stackmobElt = responseElt.getAsJsonObject().get("stackmob");
                    if(stackmobElt != null && stackmobElt.isJsonObject()) {
                        // Return only the user to be compatible with the old login
                        JsonElement userElt = stackmobElt.getAsJsonObject().get("user");
                        finalResponseBody = userElt.toString().getBytes();
                    }
                }
                callback.setDone(requestVerb, requestURL, requestHeaders, requestBody, responseStatusCode, responseHeaders, finalResponseBody);
            }
        };
    }

    @Override
    protected String getPath() {
        return "/" + session.getUserObjectName() + "/" + methodName;
    }

    @Override
    protected String getContentType() {
        return "application/x-www-form-urlencoded";
    }

    @Override
    protected String getRequestBody() {
        return formatQueryString(bodyParams);
    }
}
