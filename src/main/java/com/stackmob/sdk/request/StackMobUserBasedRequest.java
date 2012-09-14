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
import com.stackmob.sdk.net.HttpVerb;
import com.stackmob.sdk.net.HttpVerbWithoutPayload;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class StackMobUserBasedRequest extends StackMobRequest {

    private Object requestObject;

    public StackMobUserBasedRequest(ExecutorService executor,
                                    StackMobSession session,
                                    String method,
                                    List<Map.Entry<String, String>> params,
                                    StackMobRawCallback cb,
                                    StackMobRedirectedCallback redirCb) {
        super(executor, session, HttpVerbWithoutPayload.GET, StackMobRequest.EmptyHeaders, params, method, cb, redirCb);
        isSecure = true;
    }

    public StackMobUserBasedRequest(ExecutorService executor,
                                    StackMobSession session,
                                    HttpVerb verb,
                                    List<Map.Entry<String, String>> headers,
                                    List<Map.Entry<String, String>> params,
                                    Object requestObject,
                                    String method,
                                    StackMobRawCallback cb,
                                    StackMobRedirectedCallback redirCb) {
        super(executor, session, verb, headers, params, method, cb, redirCb);
        this.isSecure = true;
        this.requestObject = requestObject;
    }

    @Override
    protected String getPath() {
        return "/" + session.getUserObjectName() + "/" + methodName;
    }

    @Override
    public StackMobUserBasedRequest setUrlFormat(String urlFormat) {
        this.urlFormat = urlFormat;
        return this;
    }

    @Override protected String getRequestBody() {
        if(this.requestObject != null) {
            return gson.toJson(this.requestObject);
        }
        return "";
    }
}
