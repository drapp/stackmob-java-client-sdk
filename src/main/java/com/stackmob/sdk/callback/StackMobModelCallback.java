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

import com.stackmob.sdk.net.HttpVerb;
import com.stackmob.sdk.util.Http;

import java.util.List;
import java.util.Map;

/**
 * A simple callback suitable for many {@link com.stackmob.sdk.model.StackMobModel} methods. The success call takes no parameters since any new information will
 * be reflected in the model class
 */
public abstract class StackMobModelCallback extends StackMobCallback {
    /**
     * override this method to be notified of success after a callback. Any changes caused by the action will now
     * be reflected in the model class and/or on the server.
     */
    abstract public void success();

    public void success(String count){
        success();
    }
}
