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

/**
 * A callback that allows you to inject some logic before the actual callback is called. This is useful when you want to reuse some standard
 * callbacks but inject some custom logic in various places. This is a specialized class and most apps will never need it.
 */
public class StackMobIntermediaryCallback extends StackMobCallback {
    private StackMobCallback furtherCallback;

    /**
     * Create a callback that by default just passes through to the given callback
     * @param furtherCallback the callback that should receive messages in the end
     */
    public StackMobIntermediaryCallback(StackMobCallback furtherCallback) {
        this.furtherCallback = furtherCallback;
    }

    public StackMobCallback getFurtherCallback() {
        return this.furtherCallback;
    }

    /**
     * override and call super to inject custom logic before success
     * @param responseBody the response string received from StackMob
     */
    @Override
    public void success(String responseBody) {
        furtherCallback.success(responseBody);
    }

    /**
     * override and call super to inject custom logic before failure
     * @param e a representation of the error that occurred
     */
    @Override
    public void failure(StackMobException e) {
        furtherCallback.failure(e);
    }
}
