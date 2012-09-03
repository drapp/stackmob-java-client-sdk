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
import com.stackmob.sdk.util.StackMobCookieManager;
import com.stackmob.sdk.util.StackMobLogger;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Represent information about a users's login with StackMob. This class is only meant to be used within the SDK
 */
public class StackMobSession {

    private static String SIGNATURE_ALGORITHM = "HmacSHA1";

    private String key;
    private String secret;
    private String userObjectName;
    private String userIdName;
    private int apiVersionNumber;
    private String lastUserLoginName;
    private long serverTimeDiff = 0;
    private OAuthVersion oauthVersion;
    private String oauth2Token;
    private String oauth2MacKey;
    private String oauth2RefreshToken;
    private Date oauth2TokenExpiration;
    private Boolean httpsOverride = null;
    private StackMobCookieManager cookieManager = new StackMobCookieManager();
    private StackMobLogger logger = new StackMobLogger();

    public StackMobSession(OAuthVersion oauthVersion, int apiVersionNumber, String key, String secret, String userObjectName, String userIdName) {
        this.oauthVersion = oauthVersion;
        this.key = key;
        this.secret = secret;
        this.userObjectName = userObjectName;
        this.userIdName = userIdName;
        this.apiVersionNumber = apiVersionNumber;
    }

    public StackMobSession(StackMobSession that) {
        this.oauthVersion = that.oauthVersion;
        this.key = that.key;
        this.secret = that.secret;
        this.userObjectName = that.userObjectName;
        this.apiVersionNumber = that.apiVersionNumber;
        this.serverTimeDiff = that.serverTimeDiff;
        this.oauth2Token = that.oauth2Token;
        this.oauth2MacKey = that.oauth2MacKey;
        this.oauth2TokenExpiration = that.oauth2TokenExpiration;
        this.cookieManager = that.cookieManager;
        this.logger = logger;
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

    public String getUserIdName() {
        return userIdName;
    }

    public int getApiVersionNumber() {
        return apiVersionNumber;
    }

    protected long getLocalTime() {
        return new Date().getTime() / 1000;
    }

    public long getServerTime() {
        if(getServerTimeDiff() != 0) {
            logger.logDebug("Adjusting time for server by %d seconds", getServerTimeDiff());
        }
        return getServerTimeDiff() + getLocalTime();
    }

    public void recordServerTimeDiff(String timeHeader) {
        logger.logDebug("Got a time header of: %s", timeHeader);
        try {
            long serverTime = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(timeHeader).getTime() / 1000;
            logger.logDebug("Got a server time of %d versus local time %d", serverTime, getLocalTime());
            saveServerTimeDiff(serverTime - getLocalTime());
        } catch(Exception ignore) { }
    }

    protected void saveServerTimeDiff(long serverTimeDiff) {
        this.serverTimeDiff = serverTimeDiff;
    }

    protected long getServerTimeDiff() {
        return serverTimeDiff;
    }

    public void setLastUserLoginName(String username) {
        lastUserLoginName = username;
    }

    public String getLastUserLoginName() {
        return lastUserLoginName;
    }

    public void setEnableHTTPS(boolean enableHTTPS) {
        this.httpsOverride = enableHTTPS;
    }

    public void setHTTPSOverride(Boolean enableHTTPS) {
        this.httpsOverride = enableHTTPS;
    }

    public Boolean getHTTPSOverride() {
        return httpsOverride;
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

    public void setOAuth2TokensAndExpiration(String accessToken, String macKey, String refreshToken, int seconds) {
        setOAuth2TokensAndExpiration(accessToken, macKey, refreshToken, new Date(new Date().getTime() + seconds * 1000));
    }

    protected void setOAuth2TokensAndExpiration(String accessToken, String macKey, String refreshToken, Date expiration) {
        oauth2Token = accessToken;
        oauth2MacKey = macKey;
        oauth2RefreshToken = refreshToken;
        oauth2TokenExpiration = expiration;
    }

    public Date getOAuth2TokenExpiration() {
        return oauth2TokenExpiration;
    }

    public boolean oauth2TokenValid() {
        return oauth2TokenExpiration != null && oauth2TokenExpiration.after(new Date());
    }

    public boolean oauth2RefreshTokenValid() {
        return oauth2RefreshToken != null;
    }

    public String getOAuth2RefreshToken() {
        return oauth2RefreshToken;
    }


    public void setCookieManager(StackMobCookieManager store) {
        cookieManager = store;
    }

    public StackMobCookieManager getCookieManager() {
        return cookieManager;
    }

    /**
     * Set a custom logger to log events. The defaults are System.out in the java sdk and logcat on Android
     * @param logger the logger to use
     */
    public void setLogger(StackMobLogger logger) {
        this.logger = logger;
    }

    /**
     * Access the current logger
     * @return the logger being used to receive events
     */
    public StackMobLogger getLogger() {
        return logger;
    }

    public String generateMacToken(String method, String uri, String host, String port) {

        String ts = String.valueOf(new Date().getTime()/1000);
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
            throw new IllegalStateException("This device doesn't have SHA1");
        }
    }

    private String getNormalizedRequestString(String timestamp, String nonce, String method,
                                              String uri, String host, String port) {
        return new StringBuilder(timestamp).append("\n").append(nonce).append("\n").append(method).append("\n")
                .append(uri).append("\n").append(host).append("\n").append(port).append("\n\n").toString();
    }

}