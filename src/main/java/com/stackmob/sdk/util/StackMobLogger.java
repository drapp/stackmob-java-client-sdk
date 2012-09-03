/*
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
package com.stackmob.sdk.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Logs messages to System.out. When set in {@link com.stackmob.sdk.api.StackMob#setLogger(StackMobLogger)}, this class will be used
 * to log helpful messages. It does nothing unless enabled with {@link #setLogging(boolean)}. This class can be
 * overridden on platforms to log to the appropriate location
 */
public class StackMobLogger {
    
    private boolean enableLogging = false;

    /**
     * enables or diables actual logging. By default it is disabled.
     * @param logging whether to log
     */
    public void setLogging(boolean logging) {
        enableLogging = logging;
    }


    /**
     * log a message with debug priority
     * @param format the format
     * @param args arguments for the format
     */
    public void logDebug(String format, Object... args) {
        if(enableLogging) System.out.println(String.format(format, args));
    }

    /**
     * log a message with info priority
     * @param format the format
     * @param args arguments for the format
     */
    public void logInfo(String format, Object... args) {
        if(enableLogging) System.out.println(String.format(format, args));
    }

    /**
     * log a message with warn priority
     * @param format the format
     * @param args arguments for the format
     */
    public void logWarning(String format, Object... args) {
        if(enableLogging) System.out.println(String.format(format, args));
    }

    /**
     * log a message with error priority
     * @param format the format
     * @param args arguments for the format
     */
    public void logError(String format, Object... args) {
        if(enableLogging) System.err.println(String.format(format, args));
    }

    /**
     * turns a stacktrace into a string. Inexplicably this can't be done from the throwable
     * @param t a throwable
     * @return the stack trace from the throwable
     */
    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
