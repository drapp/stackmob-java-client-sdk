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

package com.stackmob.sdk.api;

/**
 * Represents the success or failure of an attempt to send a request. This only captures the sending, not the response
 */
public class StackMobRequestSendResult {
    /**
     * Sent or Failed
     */
    public static enum RequestSendStatus {
        SENT,
        FAILED
    }

    private RequestSendStatus status;
    private Throwable failureReason;

    /**
     * create a failed result
     * @param status sent or failed
     * @param failureReason what went wrong
     */
    public StackMobRequestSendResult(RequestSendStatus status, Throwable failureReason) {
        this.status = status;
        this.failureReason = failureReason;
    }

    /**
     * create a successful result
     */
    public StackMobRequestSendResult() {
        this(RequestSendStatus.SENT, null);
    }

    /**
     * get the status of the send
     * @return the RequestSendStatus representing what happened after the send was attempted
     */
    public RequestSendStatus getStatus() {
        return this.status;
    }

    /**
     * get the reason for the send failure
     * @return a throwable representing the reason for the send failure, or null if there was no send failure
     */
    public Throwable getFailureReason() {
        return this.failureReason;
    }
}
