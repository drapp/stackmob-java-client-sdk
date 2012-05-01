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
package com.stackmob.sdk.model;

import com.stackmob.sdk.StackMobTestCommon;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.concurrencyutils.MultiThreadAsserter;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.testobjects.Author;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static com.stackmob.sdk.concurrencyutils.CountDownLatchUtils.latch;
import static com.stackmob.sdk.concurrencyutils.CountDownLatchUtils.latchOne;

public class StackMobUserTests extends StackMobTestCommon {

    final MultiThreadAsserter asserter = new MultiThreadAsserter();
    final CountDownLatch latch = latchOne();

    public static class User extends StackMobUser {
        public String email;

        public User(String username, String password) {
            super(User.class, username, password);
        }
    }

    @Test
    public void testLoginLogout() throws Exception {
        final User user = new User("foo", "bar");
        user.login(new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                asserter.markNotNull(user.getID());
                asserter.markTrue(user.password == null);
                asserter.markNotNull(user.email);
                user.logout(new StackMobCallback() {
                    @Override
                    public void success(String responseBody) {
                        latch.countDown();
                    }

                    @Override
                    public void failure(StackMobException e) {
                        asserter.markException(e);
                    }
                });
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }
}
