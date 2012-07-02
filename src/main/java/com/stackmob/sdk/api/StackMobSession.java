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

import java.text.SimpleDateFormat;
import java.util.Date;
import com.stackmob.sdk.api.StackMob.OAuthVersion;

public class StackMobSession {

    private String key;
    private String secret;
    private String userObjectName;
    private int apiVersionNumber;
    private String appName = null;
    private String lastUserLoginName;
    private long serverTimeDiff = 0;
    private OAuthVersion oauthVersion;
    private String oauth2Token;
    private Date oauth2TokenExpiration;
    private boolean enableHTTPS = true;

    public StackMobSession(OAuthVersion oauthVersion, String key, String secret, String userObjectName, String appName, int apiVersionNumber) {
        this(oauthVersion, key, secret, userObjectName, apiVersionNumber, null);
    }

    public StackMobSession(String key, String secret, String userObjectName, String appName, int apiVersionNumber) {
        this(key, secret, userObjectName, apiVersionNumber, null);
    }

    public StackMobSession(String key, String secret, String userObjectName, int apiVersionNumber, String appName) {
        this(OAuthVersion.One, key, secret, userObjectName, apiVersionNumber, appName);
    }

    public StackMobSession(OAuthVersion oauthVersion, String key, String secret, String userObjectName, int apiVersionNumber, String appName) {
        if(key.equals(StackMobConfiguration.DEFAULT_API_KEY) || secret.equals(StackMobConfiguration.DEFAULT_API_SECRET)) {
            throw new RuntimeException("You forgot to set your api key and secret");
        }
        this.oauthVersion = oauthVersion;
        this.key = key;
        this.secret = secret;
        this.userObjectName = userObjectName;
        this.apiVersionNumber = apiVersionNumber;
        this.appName = appName;
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

    public long getServerTime() {
        if(getServerTimeDiff() != 0) {
            StackMob.getLogger().logDebug("Adjusting time for server by %d seconds", getServerTimeDiff());
        }
        return getServerTimeDiff() + getLocalTime();
    }

    public void recordServerTimeDiff(String timeHeader) {
        StackMob.getLogger().logDebug("Got a time header of: %s", timeHeader);
        try {
            long serverTime = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(timeHeader).getTime() / 1000;
            StackMob.getLogger().logDebug("Got a server time of %d versus local time %d", serverTime, getLocalTime());
            saveServerTimeDiff(serverTime - getLocalTime());
        } catch(Exception ignore) { }
    }

    protected void saveServerTimeDiff(long serverTimeDiff) {
        this.serverTimeDiff = serverTimeDiff;
    }

    protected long getServerTimeDiff() {
        return serverTimeDiff;
    }

    protected void setLastUserLoginName(String username) {
        lastUserLoginName = username;
    }

    protected String getLastUserLoginName() {
        return lastUserLoginName;
    }

    public void setEnableHTTPS(boolean enableHTTPS) {
        this.enableHTTPS = enableHTTPS;
    }

    public boolean getEnableHTTPS() {
        return enableHTTPS;
    }

    public OAuthVersion getOAuthVersion() {
        return oauthVersion;
    }

    public boolean isOAuth2() {
        return oauthVersion == StackMob.OAuthVersion.Two;
    }

    public String getOAuth2Token() {
        return oauth2Token;
    }

    public void setOAuth2Token(String oauth2Token) {
        this.oauth2Token = oauth2Token;
    }

    public void setOAuth2TokenExpiration(int seconds) {
        oauth2TokenExpiration = new Date(new Date().getTime() + seconds * 1000);
    }

    public Date getOAuth2TokenExpiration() {
        return oauth2TokenExpiration;
    }

}