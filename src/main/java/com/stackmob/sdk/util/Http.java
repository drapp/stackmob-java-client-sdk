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

package com.stackmob.sdk.util;

public class Http {
    //the lowest HTTP error code (inclusive) that represents a success
    public static final Integer SuccessResponseLowerLimit = 100;
    //the highest HTTP error code (non inclusive) that represents a success
    public static final Integer SuccessResponseUpperLimit = 400;
    //the HTTP error code that represents unavailable
    public static final Integer UnavailableResponseCode = 503;
    //the lowercase version of the "Retry-After" header
    public static final String RetryAfterLowercase = "retry-after";

    /**
     * determine whether a given status code represents a success
     * @param statusCode the status code to test
     * @return true if it's a success, false otherwise
     */
    public static boolean isSuccess(Integer statusCode) {
        return (statusCode < SuccessResponseUpperLimit) && (statusCode >= SuccessResponseLowerLimit);
    }

    public static boolean isUnavailable(Integer statusCode) {
        return statusCode != null && statusCode.equals(UnavailableResponseCode);
    }

    public static boolean isRetryAfterHeader(String headerName) {
        return headerName != null && headerName.toLowerCase().equals(RetryAfterLowercase);
    }
}
