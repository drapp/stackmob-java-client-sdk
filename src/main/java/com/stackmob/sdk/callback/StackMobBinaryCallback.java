/**
 * Copyright 2011 StackMob
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

package com.stackmob.sdk.callback;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.exception.StackMobHTTPResponseException;
import com.stackmob.sdk.net.HttpVerb;
import com.stackmob.sdk.util.Http;

import java.util.List;
import java.util.Map;

/**
 * A callback class that allows access to the raw byte array of the response body.  This is helpful when the body is
 * binary payload such as a generic String or image.
 */
public abstract class StackMobBinaryCallback extends StackMobRawCallback {

    @Override
    public void unsent(StackMobException e) {
        failure(e);
    }

    @Override public void temporaryPasswordResetRequired(StackMobException e) {
        failure(e);
    }

    @Override
    public void done(HttpVerb requestVerb,
                     String requestURL,
                     List<Map.Entry<String, String>> requestHeaders,
                     String requestBody,
                     Integer responseStatusCode,
                     List<Map.Entry<String, String>> responseHeaders,
                     byte[] responseBody) {
        if(Http.isSuccess(responseStatusCode)) {
            success(responseBody);
        } else {
            StackMobException smException = new StackMobHTTPResponseException(responseStatusCode, responseHeaders, responseBody);
            if(isTemporaryPasswordMessage("error") || isTemporaryPasswordMessage("error_description")) {
                temporaryPasswordResetRequired(smException);
            } else {
                failure(smException);
            }
        }
    }

    private boolean isTemporaryPasswordMessage(String name) {
        try {
            String test = new String(responseBody, "UTF-8");
            JsonElement message = new JsonParser().parse(new String(responseBody, "UTF-8")).getAsJsonObject().get(name);
            return message != null && message.isJsonPrimitive() && message.getAsJsonPrimitive().isString() &&
                    message.getAsString().startsWith("Temporary password reset required.");
        } catch(Exception e) {
            return false;
        }
    }

    /**
     * override this method to handles cases where a call has succeeded.
     * @param responseBody the response binary received from StackMob
     */
    abstract public void success(byte[] responseBody);

    /**
     * override this method to handle errors
     * @param e a representation of the error that occurred
     */
    abstract public void failure(StackMobException e);
}
