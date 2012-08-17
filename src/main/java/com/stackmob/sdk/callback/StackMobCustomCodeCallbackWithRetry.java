package com.stackmob.sdk.callback;

import com.stackmob.sdk.exception.StackMobHTTPResponseException;
import com.stackmob.sdk.net.HttpVerb;
import com.stackmob.sdk.util.Http;

import java.util.List;
import java.util.Map;

public abstract class StackMobCustomCodeCallbackWithRetry extends StackMobCustomCodeCallback {
    public static final int defaultRetryAfterMilliseconds = 30000;

    /**
     * the method that will be called when a retry is necessary.
     * retries are necessary in custom code if a request is made and your custom code instance is starting up
     * @param afterMilliseconds the number of milliseconds to wait until retrying the request.
     *                          you are advised not to make requests before that time period has passed,
     *                          as custom code will continue to return UNAVAILABLE responses until it is
     */
    public abstract void retry(int afterMilliseconds);

    @Override
    public void done(HttpVerb requestVerb,
                     String requestURL,
                     List<Map.Entry<String, String>> requestHeaders,
                     String requestBody,
                     Integer responseStatusCode,
                     List<Map.Entry<String, String>> responseHeaders,
                     byte[] responseBody) {
        if(Http.isUnavailable(responseStatusCode)) {
            int afterMilliseconds = defaultRetryAfterMilliseconds;
            for(Map.Entry<String, String> headerPair : requestHeaders) {
                if(Http.isRetryAfterHeader(headerPair.getKey())) {
                    try {
                        int candidateMilliseconds = Integer.parseInt(headerPair.getValue());
                        if(candidateMilliseconds > 0) {
                            afterMilliseconds = candidateMilliseconds;
                        }
                    } catch(Throwable t) {
                        //do nothing
                    }
                }
            }
            retry(afterMilliseconds);
        }
        else if(Http.isSuccess(responseStatusCode)) {
            success(new String(responseBody));
        }
        else {
            failure(new StackMobHTTPResponseException(responseStatusCode, responseHeaders, responseBody));
        }
    }
}
