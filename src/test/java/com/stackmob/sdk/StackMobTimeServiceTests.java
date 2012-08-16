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

import com.stackmob.sdk.net.StackMobApi;
import org.junit.Test;
import org.scribe.services.TimestampService;

import static org.junit.Assert.assertNotSame;

public class StackMobTimeServiceTests {
    @Test
    public void testDistinctUniqueNonceGeneration() {
        TimestampService ts = new StackMobApi.StackMobTimeService();
        String nonce1 = ts.getNonce();
        String nonce2 = ts.getNonce();

        assertNotSame(nonce1, nonce2);
    }
}
