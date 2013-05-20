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
 * The basic callback class for responding to asynchronous StackMob calls. Most methods in the {@link com.stackmob.sdk.api.StackMob}
 * class take one of these as an argument. It's standard practice to subclass on the fly.
 * <pre>
 * {@code
 * stackmob.doSomething(argument, new StackMobCallback() {
 *     public void success(String responseBody) {
 *         assertNotNull(responseBody);
 *         latch.countDown();
 *     }
 *
 *     public void failure(StackMobException e) {
 *         fail(e.getMessage());
 *     }
 * }
 * }
 * </pre>
 * When a callback is invoked you will generally not be in the main thread, so react accordingly.
 */
public abstract class StackMobCallback extends StackMobRawCallback {

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
            success(new String(responseBody));
        } else {
            StackMobException smException = new StackMobHTTPResponseException(responseStatusCode, responseHeaders, responseBody);
            try {
                JsonElement errorDescription = new JsonParser().parse(new String(responseBody, "UTF-8")).getAsJsonObject().get("error_description");
                if(errorDescription != null &&
                        errorDescription.isJsonPrimitive() &&
                        errorDescription.getAsJsonPrimitive().isString() &&
                        errorDescription.getAsString().startsWith("Temporary password reset required.")) {
                    temporaryPasswordResetRequired(smException);
                } else {
                    failure(smException);
                }
            } catch(Exception e) {
                failure(smException);
            }
        }
    }

    /**
     * override this method to handles cases where a call has succeeded.
     * @param responseBody the response string received from StackMob
     */
    abstract public void success(String responseBody);

    /**
     * override this method to handle errors
     * @param e a representation of the error that occurred
     */
    abstract public void failure(StackMobException e);
}
