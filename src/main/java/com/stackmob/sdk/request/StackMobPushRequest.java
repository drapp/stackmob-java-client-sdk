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
package com.stackmob.sdk.request;

import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.api.StackMobOptions;
import com.stackmob.sdk.api.StackMobSession;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.net.HttpVerbWithPayload;
import com.stackmob.sdk.request.StackMobRequestWithPayload;

import java.util.concurrent.ExecutorService;

public class StackMobPushRequest extends StackMobRequestWithPayload {

    public StackMobPushRequest(ExecutorService executor, StackMobSession session, Object requestObject, String method, StackMobRawCallback cb, StackMobRedirectedCallback redirCb) {
        super(executor, session, HttpVerbWithPayload.POST, StackMobOptions.none(), EmptyParams, requestObject, method, cb, redirCb);
    }

    @Override
    protected StackMob.OAuthVersion getOAuthVersion() {
        return StackMob.OAuthVersion.One;
    }
}
