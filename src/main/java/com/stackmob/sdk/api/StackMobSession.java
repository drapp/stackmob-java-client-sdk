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

import com.google.gson.Gson;

import java.util.Date;

public class StackMobSession {
    private String key;
    private String secret;
    private String userObjectName;
    private int apiVersionNumber;
    private String appName = null;
    private long nextTimeCheck = 0;
    private long serverTimeDiff = 0;

    public StackMobSession(String key, String secret, String userObjectName, String appName, int apiVersionNumber) {
        this(key, secret, userObjectName, apiVersionNumber);
        this.appName = appName;
    }

    public StackMobSession(String key, String secret, String userObjectName, int apiVersionNumber) {
        if(key.equals(StackMobConfiguration.DEFAULT_API_KEY) || secret.equals(StackMobConfiguration.DEFAULT_API_SECRET)) {
            throw new RuntimeException("You forgot to set your api key and secret");
        }
        this.key = key;
        this.secret = secret;
        this.userObjectName = userObjectName;
        this.apiVersionNumber = apiVersionNumber;
    }

    public StackMobSession(StackMobSession that) {
        this.key = that.key;
        this.secret = that.secret;
        this.appName = that.appName;
        this.userObjectName = that.userObjectName;
        this.apiVersionNumber = that.apiVersionNumber;
    }

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }

    public String getUserObjectName() {
        return userObjectName;
    }

    public int getApiVersionNumber() {
        return apiVersionNumber;
    }

    public String getAppName() {
        return appName;
    }

    protected long getLocalTime() {
        return new Date().getTime() / 1000;
    }

    public boolean timeCheckRequired() {
        return nextTimeCheck < getLocalTime();
    }

    public long getServerTime() {
        return getServerTimeDiff() + getLocalTime();
    }
    
    public void recordServerTimeDiff(String timeHeader) {
        try {
            saveServerTimeDiff(Long.parseLong(timeHeader) - getLocalTime());
            //Set the next sync for 10 minutes
            nextTimeCheck = getLocalTime() + 10 * 60;
        } catch(Exception ignore) {}
    }

    protected void saveServerTimeDiff(long serverTimeDiff) {
        this.serverTimeDiff = serverTimeDiff;
    }

    protected long getServerTimeDiff() {
        return serverTimeDiff;
    }

}