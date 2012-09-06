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

package com.stackmob.sdk;

import com.google.gson.Gson;
import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.callback.StackMobRedirectedCallback;
import com.stackmob.sdk.concurrencyutils.MultiThreadAsserter;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.testobjects.Error;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.stackmob.sdk.testobjects.StackMobObject;
import com.stackmob.sdk.testobjects.StackMobObjectOnServer;

import static org.junit.Assert.*;

public class StackMobTestCommon {

    private static final String ENVIRONMENT_KEY_KEY = "STACKMOB_KEY";
    private static final String ENVIRONMENT_SECRET_KEY = "STACKMOB_SECRET";
    private static final String ENVIRONMENT_DISABLE_HTTPS_KEY = "STACKMOB_DISABLE_HTTPS";

    protected static final Gson gson = new Gson();
    protected final StackMob stackmob;

    public StackMobTestCommon() {
        String apiKey = "API_KEY";
        String apiSecret = "API_SECRET";
        int apiVersion = 0;

        String envKey = System.getenv(ENVIRONMENT_KEY_KEY);
        String envSecret = System.getenv(ENVIRONMENT_SECRET_KEY);
        String envHTTPS = System.getenv(ENVIRONMENT_DISABLE_HTTPS_KEY);
        if(envKey != null && envSecret != null) {
            System.out.println("found environment vars for key & secret. using these");
            apiKey = envKey;
            apiSecret = envSecret;
        }
        String vmKey = System.getProperty(ENVIRONMENT_KEY_KEY);
        String vmSecret = System.getProperty(ENVIRONMENT_SECRET_KEY);
        if(vmKey != null && vmSecret != null) {
            System.out.println("found JVM args for key & secret. using these & overriding previous");
            apiKey = vmKey;
            apiSecret = vmSecret;
        }

        StackMob.setStackMob(new StackMob(StackMob.OAuthVersion.One, apiVersion, apiKey, apiSecret));
            StackMob.getStackMob().getSession().setEnableHTTPS(false);
        StackMob.getStackMob().getSession().getLogger().setLogging(true);
        stackmob = StackMob.getStackMob();
    }

    public static void assertNotError(String responseBody) {
        try {
            Error err = gson.fromJson(responseBody, Error.class);
            assertNull("request failed with error: " + err.error, err.error);
        }
        catch (Exception e) {
            //do nothing
        }
    }

    public static boolean isError(String responseBody) {
        Error err = gson.fromJson(responseBody, Error.class);
        return err.error != null;
    }

    protected String getRandomString() {
        return UUID.randomUUID().toString();
    }

    protected <T extends StackMobObject> StackMobObjectOnServer<T> createOnServer(final T obj, final Class<T> cls) throws StackMobException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final MultiThreadAsserter asserter = new MultiThreadAsserter();

        final AtomicReference<String> ref = new AtomicReference<String>(null);

        stackmob.getDatastore().post(obj.getName(), obj, new StackMobCallback() {
            @Override public void success(String responseBody) {
                if(!asserter.markNotJsonError(responseBody)) {
                    try {
                        T obj = gson.fromJson(responseBody, cls);
                        String idField = obj.getIdField();
                        ref.set(idField);
                    }
                    catch(Throwable e) {
                        asserter.markException(e);
                    }
                }
                latch.countDown();
            }

            @Override public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });

        asserter.assertLatchFinished(latch);
        return new StackMobObjectOnServer<T>(stackmob, ref.get(), obj);
    }
}
