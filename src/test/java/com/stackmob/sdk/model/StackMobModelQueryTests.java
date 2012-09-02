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
import com.stackmob.sdk.api.StackMobQuery;
import com.stackmob.sdk.api.StackMobQueryField;
import com.stackmob.sdk.callback.StackMobCountCallback;
import com.stackmob.sdk.callback.StackMobQueryCallback;
import com.stackmob.sdk.concurrencyutils.MultiThreadAsserter;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.testobjects.Author;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.stackmob.sdk.concurrencyutils.CountDownLatchUtils.latchOne;

public class StackMobModelQueryTests extends StackMobTestCommon {

    final MultiThreadAsserter asserter = new MultiThreadAsserter();
    final CountDownLatch latch = latchOne();

    @Test public void testQuery() throws Exception {
        StackMobModel.query(Author.class, new StackMobQuery().isInRange(0,10), new StackMobQueryCallback<Author>() {
            @Override
            public void success(List<Author> result) {
                asserter.markEquals(11, result.size());
                asserter.markNotNull(result.get(0).getName());
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void testStaticClassQuery() throws Exception {
        Author.query(Author.class, new StackMobQuery("author").isInRange(0, 10), new StackMobQueryCallback<Author>() {
            @Override
            public void success(List<Author> result) {
                asserter.markEquals(11, result.size());
                asserter.markNotNull(result.get(0).getName());
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void testFieldQuery() throws Exception {
        StackMobModel.query(Author.class, new StackMobQuery().isInRange(0, 10).field(new StackMobQueryField("name").isEqualTo("testqueryauthor")), new StackMobQueryCallback<Author>() {
            @Override
            public void success(List<Author> result) {
                asserter.markEquals(3, result.size());
                asserter.markNotNull(result.get(0).getName());
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void testCount() throws Exception {
        Author.count(new StackMobQuery().isInRange(0,10).field(new StackMobQueryField("name").isEqualTo("testqueryauthor")), new StackMobCountCallback() {
            @Override
            public void success(long count) {
                asserter.markEquals(3, (int)count);
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void testNotEqualQuery() throws Exception {
        Author.query(Author.class, new StackMobQuery().fieldIsNotEqual("name", "tolstoy"), new StackMobQueryCallback<Author>() {
            @Override
            public void success(List<Author> result) {
                asserter.markTrue(result.size() > 0);
                for (Author a : result) {
                    asserter.markFalse("tolstoy".equals(a.getName()));
                }
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void testIsNullQuery() throws Exception {
        Author.query(Author.class, new StackMobQuery().fieldIsNull("name"), new StackMobQueryCallback<Author>() {
            @Override
            public void success(List<Author> result) {
                asserter.markTrue(result.size() > 0);
                for (Author a : result) {
                    asserter.markTrue(a.getName() == null);
                }
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    @Test public void testIsNotNullQuery() throws Exception {
        Author.query(Author.class, new StackMobQuery().fieldIsNotNull("name"), new StackMobQueryCallback<Author>() {
            @Override
            public void success(List<Author> result) {
                asserter.markTrue(result.size() > 0);
                for (Author a : result) {
                    asserter.markNotNull(a.getName());
                }
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);
    }

    private static class User extends StackMobModel {
        public User(String username) {
            super(User.class);
        }

        @Override
        public String getIDFieldName() {
            return "username";
        }
    }
    
    @Test public void testDefaultCtor() throws Exception {
        StackMobQuery loginUserQuery = new StackMobQuery();
        loginUserQuery.fieldIsEqualTo("username", "drapp");
        User.query(User.class, loginUserQuery, new StackMobQueryCallback<User>() {
            @Override
            public void success(List<User> result) {
                asserter.markEquals(1, result.size());
                asserter.markNotNull(result.get(0).getID());
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                asserter.markException(e);
            }
        });
        asserter.assertLatchFinished(latch);

    }
}
