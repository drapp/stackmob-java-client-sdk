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

package com.stackmob.sdk.testobjects;

import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.exception.StackMobException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static com.stackmob.sdk.concurrencyutils.CountDownLatchUtils.*;

public class StackMobObjectOnServer<T extends StackMobObject> {
    private T obj;
    private String objectId;
    private StackMob stackmob;

    public StackMobObjectOnServer(final StackMob stackmob, final String objectId, final T obj) {
        this.objectId = objectId;
        this.obj = obj;
        this.stackmob = stackmob;
    }

    public String getObjectId() {
        return this.objectId;
    }

    public T getObject() {
        return this.obj;
    }

    public StackMob getStackmob() {
        return this.stackmob;
    }

    public void delete() throws StackMobException {
        final String objectName = obj.getName();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean errorBool = new AtomicBoolean();
        final AtomicReference<StackMobException> exception = new AtomicReference<StackMobException>(null);

        stackmob.getDatastore().delete(objectName, objectId, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                if(!"Successfully deleted document".equals(responseBody)) {
                    errorBool.set(true);
                    exception.set(new StackMobException(obj.getName() + " " + objectId + " not successfully deleted"));
                }
                latch.countDown();
            }

            @Override
            public void failure(StackMobException e) {
                errorBool.set(true);
                exception.set(e);
                latch.countDown();
            }
        });

        try {assertLatchFinished(latch, errorBool, exception);} catch(InterruptedException e) {/*ignore*/}
    }
}
