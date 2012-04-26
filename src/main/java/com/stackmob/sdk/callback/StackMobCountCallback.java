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
package com.stackmob.sdk.callback;

import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.exception.StackMobHTTPResponseException;
import com.stackmob.sdk.net.HttpVerb;
import com.stackmob.sdk.util.Http;

import java.util.List;
import java.util.Map;

public abstract class StackMobCountCallback extends StackMobCallback{
    @Override
    public void done(HttpVerb requestVerb,
                               String requestURL,
                               List<Map.Entry<String, String>> requestHeaders,
                               String requestBody,
                               Integer responseStatusCode,
                               List<Map.Entry<String, String>> responseHeaders,
                               byte[] responseBody) {
        super.done(requestVerb, requestURL, requestHeaders, requestBody, responseStatusCode, responseHeaders, responseBody);
        if(Http.isSuccess(responseStatusCode)) {
            success(Long.valueOf(new String(responseBody)));
        }

    }

    abstract public void success(long count);

    public void success(String count){}

}
