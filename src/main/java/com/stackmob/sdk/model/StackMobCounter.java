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

/**
 * Represents a integer that can be incremented atomically. Its purpose is to allow you to have a value that can be
 * incremented from multiple clients without worrying about synchronization. Use cases include a score tracker or
 * thumbs-up button.
 */
public class StackMobCounter {
    private int localBaseVal = 0;
    private int increment = 0;
    enum Mode{INCREMENT, SET}
    private Mode mode = Mode.INCREMENT;

    /**
     * Increment the value by a number (positive or negative). This will change the local value immediately
     * and the increment will be sent to the server as an atomic increment on the next save. The increment
     * accumulates across multiple calls, so if you call this with values of 2 and -3 before saving, the sever-side
     * value will be incremented by -1.
     * @param increment the value to change the counter by
     * @return the new value of the local counter
     */
    public synchronized int updateAtomicallyBy(int increment) {
        this.increment += increment;
        return get();
    }

    /**
     *
     * @return the current local value of the counter
     */
    public int get() {
        return localBaseVal + increment;
    }

    /**
     *
     * @return the value that will be use for incrementing the counter on the next save
     */
    public int getIncrement() {
        return increment;
    }

    /**
     * Force the counter to a particular value, overriding the increment. After calling this you can
     * then save the model, which will cause the server-side value to be set rather than incremented.
     * @param val the value the counter should be set to
     */
    public synchronized void forceTo(int val) {
        mode = Mode.SET;
        this.localBaseVal = val;
        this.increment = 0;
    }

    Mode getMode() {
        return mode;
    }

    synchronized void reset() {
        localBaseVal = get();
        increment = 0;
        mode = Mode.INCREMENT;
    }

    synchronized void set(int val) {
        localBaseVal = val;
    }
}
