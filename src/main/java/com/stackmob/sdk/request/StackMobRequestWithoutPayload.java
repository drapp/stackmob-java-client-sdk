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

import com.stackmob.sdk.api.StackMobOptions;
import com.stackmob.sdk.api.StackMobSession;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.net.HttpVerbWithoutPayload;

import java.util.concurrent.ExecutorService;
import java.util.Map;
import java.util.List;

public class StackMobRequestWithoutPayload extends StackMobRequest {
    public StackMobRequestWithoutPayload(ExecutorService executor,
                                         StackMobSession session,
                                         HttpVerbWithoutPayload verb,
                                         StackMobOptions options,
                                         List<Map.Entry<String, String>>  params,
                                         String method,
                                         StackMobRawCallback cb,
                                         StackMobRedirectedCallback redirCb) {
        super(executor, session, verb, options, params, method, cb, redirCb);
    }

    public StackMobRequestWithoutPayload(ExecutorService executor,
                                         StackMobSession session,
                                         HttpVerbWithoutPayload verb,
                                         String method,
                                         StackMobRawCallback cb,
                                         StackMobRedirectedCallback redirCb) {
        this(executor, session, verb, StackMobOptions.none(), EmptyParams, method, cb, redirCb);
    }

    @Override protected String getRequestBody() {
        return "";
    }
}