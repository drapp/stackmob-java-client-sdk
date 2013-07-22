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

import java.util.Map;

/**
 * Override the redirected method on your StackMobCallback instead
 */
@Deprecated()
public interface StackMobRedirectedCallback {
    /**
     * Invoked when a redirect has been issued
     * @param originalUrl the url being redirected from
     * @param redirectHeaders headers that came with the redirect
     * @param redirectBody the body that came with the redirect
     * @param newURL the url being redirected to
     */
    void redirected(String originalUrl, Map<String, String> redirectHeaders, String redirectBody, String newURL);
}
