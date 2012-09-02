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

package com.stackmob.sdk.request;

import com.stackmob.sdk.api.StackMobSession;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.net.HttpVerbWithPayload;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class StackMobRequestWithPayload extends StackMobRequest {
    private String body;
    private Object requestObject;

    public StackMobRequestWithPayload(ExecutorService executor,
                                      StackMobSession session,
                                      HttpVerbWithPayload verb,
                                      List<Map.Entry<String, String>> headers,
                                      Map<String, String> params,
                                      String body,
                                      String method,
                                      StackMobRawCallback cb,
                                      StackMobRedirectedCallback redirCb) {
        super(executor, session, verb, headers, params, method, cb, redirCb);
        this.body = body;
    }
    
    public StackMobRequestWithPayload(ExecutorService executor,
                                      StackMobSession session,
                                      HttpVerbWithPayload verb,
                                      List<Map.Entry<String, String>> headers,
                                      Map<String, String> params,
                                      Object requestObject,
                                      String method,
                                      StackMobRawCallback cb,
                                      StackMobRedirectedCallback redirCb) {
        super(executor, session, verb, headers, params, method, cb, redirCb);
        this.requestObject = requestObject;
    }

    public StackMobRequestWithPayload(ExecutorService executor, StackMobSession session, HttpVerbWithPayload verb, String method, StackMobRawCallback cb, StackMobRedirectedCallback redirCb) {
        this(executor, session, verb, EmptyHeaders, EmptyParams, null, method, cb, redirCb);
    }

    @Override protected String getRequestBody() {
        if(this.body != null) {
            return body;
        }
        if(this.requestObject != null) {
            return gson.toJson(this.requestObject);
        }
        return "";
    }
}
