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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.stackmob.sdk.api.StackMob.OAuthVersion;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class StackMobSession {

    private static String SIGNATURE_ALGORITHM = "HmacSHA1";

    private String key;
    private String secret;
    private String userObjectName;
    private int apiVersionNumber;
    private String appName = null;
    private String lastUserLoginName;
    private long serverTimeDiff = 0;
    private OAuthVersion oauthVersion;
    private String oauth2Token;
    private String oauth2MacKey;
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
        if(key.equals(StackMobConfiguration.DEFAULT_API_KEY) || (oauthVersion != OAuthVersion.Two && secret.equals(StackMobConfiguration.DEFAULT_API_SECRET))) {
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

    public void setOAuthVersion(OAuthVersion oauthVersion) {
        this.oauthVersion = oauthVersion;
    }

    public boolean isOAuth2() {
        return oauthVersion == StackMob.OAuthVersion.Two;
    }

    public void setOAuth2TokenAndExpiration(String accessToken, String macKey, int seconds) {
        oauth2Token = accessToken;
        oauth2MacKey = macKey;
        oauth2TokenExpiration = new Date(new Date().getTime() + seconds * 1000);
    }

    public Date getOAuth2TokenExpiration() {
        return oauth2TokenExpiration;
    }

    public boolean oauth2TokenValid() {
        return oauth2TokenExpiration != null && oauth2TokenExpiration.after(new Date());
    }

    public String generateMacToken(String method, String uri, String host, String port) {

        String ts = String.valueOf(getServerTime());
        String nonce = String.format("n%d", Math.round(Math.random() * 10000));
        try {
            String baseString = getNormalizedRequestString(ts, nonce, method, uri, host, port);
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            SecretKeySpec spec = new SecretKeySpec(oauth2MacKey.getBytes(), SIGNATURE_ALGORITHM);
            try {
                mac.init(spec);
            } catch(InvalidKeyException ike) {
                throw new IllegalStateException(ike);
            }
            byte[] rawMacBytes = mac.doFinal(baseString.getBytes());
            byte[] b64Bytes = Base64.encodeBase64(rawMacBytes);
            String calculatedMac = new String(b64Bytes);
            return String.format("MAC id=\"%s\",ts=\"%s\",nonce=\"%s\",mac=\"%s\"", oauth2Token, ts, nonce, calculatedMac);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("This device don't have SHA1");
        }
    }

    private String getNormalizedRequestString(String timestamp, String nonce, String method,
                                              String uri, String host, String port) {
        return new StringBuilder(timestamp).append("\n").append(nonce).append("\n").append(method).append("\n")
                .append(uri).append("\n").append(host).append("\n").append(port).append("\n\n").toString();
    }

}