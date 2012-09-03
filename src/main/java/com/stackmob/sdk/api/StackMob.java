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

import com.google.gson.*;
import com.stackmob.sdk.callback.*;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.net.HttpVerb;
import com.stackmob.sdk.net.HttpVerbWithPayload;
import com.stackmob.sdk.net.HttpVerbWithoutPayload;
import com.stackmob.sdk.push.StackMobPush;
import com.stackmob.sdk.push.StackMobPushToken;
import com.stackmob.sdk.request.*;
import com.stackmob.sdk.util.Http;
import com.stackmob.sdk.util.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The StackMob object is your interface for accessing StackMob's many features. Its functions include:
 * <ul>
 * <li>Logging in and managing user sessions</li>
 * <li>Datastore API Methods</li>
 * <li>Push API Requests</li>
 * </ul>
 * <p>
 * A StackMob instance is created with authorization credentials along with some optional configuration
 * parameters. To use different configurations in one app, simply instantiate multiple StackMob objects.
 */
public class StackMob {

    /**
     * The two different OAuth versions the SDK can use for authentication. The Push API currently only supports
     * OAuth1 and will use that regardless of which oauth version you're using.
     */
    public static enum OAuthVersion {
        /**
         * OAuth1 uses a private key embedded in your app to authenticate. Apps authenticated like this
         * have authorization overriding any Access Controls you may have set up. This setting is
         * recommended for admin consoles only, and not for production apps.
         */
        One,
        /**
         * OAuth2 authenticates individual users and allows user-based roles and access permissions.
         */
        Two
    }

    private StackMobSession session;
    private String userIdName;
    private String passwordField;
    private String apiUrlFormat;
    private String pushUrlFormat;
    private ExecutorService executor;


    private final Object urlFormatLock = new Object();



    private static final String versionKey= "sdk.version";
    private static String userAgentName = "Java Client";
    private static String version = null;
    private static StackMobLogger logger = new StackMobLogger();


    public static String DEFAULT_API_HOST = "api.stackmob.com";
    public static String DEFAULT_PUSH_HOST = "push.stackmob.com";
    public static String DEFAULT_USER_SCHEMA_NAME = "user";
    public static String DEFAULT_USER_ID = "username";
    public static String DEFAULT_PASSWORD_FIELD = "password";
    public static StackMobRedirectedCallback DEFAULT_REDIRECTED_CALLBACK = new StackMobRedirectedCallback() {
        @Override
        public void redirected(String originalUrl, Map<String, String> redirectHeaders, String redirectBody, String newURL) {
            // Do nothing
        }
    };

    /**
     * The current android sdk version
     * @return the android sdk version
     */
    public static String getVersion() {
        if(version == null ) {
            version = "";
            Properties props = new Properties();
            try {
                props.load(StackMob.class.getClassLoader().getResourceAsStream("build.properties"));
            } catch (IOException e) {
            } catch (NullPointerException e) { }
            if( props.containsKey(versionKey) && props.get(versionKey) != null) {
                version = props.getProperty(versionKey);
                //This should be replaced by a real version in maven builds
                if("${version}".equals(version)) version = "dev";
            }
        }
        return version;
    }

    public static String getUserAgent() {
        return String.format("StackMob (%s; %s)", userAgentName,
                getVersion());
    }

    /**
     * Override the name used in the use agent
     * @param name the name to use in the user agent
     */
    public static void setUserAgentName(String name) {
        userAgentName = name;
    }

    /**
     * The redirected callback set by the user
     */
    protected StackMobRedirectedCallback userRedirectedCallback;

    /**
     * An internal redirected callback.
     */
    protected StackMobRedirectedCallback redirectedCallback = new StackMobRedirectedCallback() {
        @Override
        public void redirected(String originalUrl, Map<String, String> redirectHeaders, String redirectBody, String newURL) {
            try {
                URI uri = new URI(newURL);
                synchronized(urlFormatLock) {
                    final String host = uri.getHost();
                    if(host.startsWith("push.") && !pushUrlFormat.equalsIgnoreCase(host)) {
                        pushUrlFormat = host;
                        userRedirectedCallback.redirected(originalUrl, redirectHeaders, redirectBody, newURL);
                    }
                    else if(host.startsWith("api.") && !apiUrlFormat.equalsIgnoreCase(host)) {
                        apiUrlFormat = host;
                        userRedirectedCallback.redirected(originalUrl, redirectHeaders, redirectBody, newURL);
                    }
                }
            }
            catch (URISyntaxException e) {
                //unable to parse new URL - do nothing
            }
        }
    };

    private static ExecutorService createNewExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Set a custom logger to log events. The defaults are System.out in the java sdk and logcat on Android
     * @param logger the logger to user
     */
    public static void setLogger(StackMobLogger logger) {
        StackMob.logger = logger;
    }

    /**
     * Access the current logger
     * @return the logger being used to receive events
     */
    public static StackMobLogger getLogger() {
        return logger;
    }
    
    private static StackMob stackmob;

    /**
     * Get the singleton StackMob object.
     * @return the singleton StackMob
     */
    public static synchronized StackMob getStackMob() {
        return stackmob;
    }

    /**
     * Set the singleton StackMob to a particular one for convenience.
     * @param stackmob the new singleton
     */
    public static void setStackMob(StackMob stackmob) {
        StackMob.stackmob = stackmob;
    }

    /**
     * a StackMob constructor allowing you to specify the OAuth version.
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     * @param apiKey the api key for your app
     */
    public StackMob(Integer apiVersionNumber, String apiKey) {
        this(OAuthVersion.Two, apiVersionNumber, apiKey, null);
    }

    /**
     * a StackMob constructor allowing you to specify the OAuth version.
     * @param oauthVersion whether to use oauth1 or oauth2
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     */
    public StackMob(OAuthVersion oauthVersion, Integer apiVersionNumber, String apiKey, String apiSecret) {
        this(oauthVersion, apiVersionNumber, apiKey, apiSecret, DEFAULT_API_HOST, DEFAULT_PUSH_HOST, DEFAULT_USER_SCHEMA_NAME, DEFAULT_USER_ID, DEFAULT_PASSWORD_FIELD, DEFAULT_REDIRECTED_CALLBACK);
    }

    /**
     * the most complete StackMob constructor allowing you to set values for everything
     * @param oauthVersion whether to use oauth1 or oauth2
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app. Can be null if you're using OAuth2
     * @param apiHost the base of the url for api requests
     * @param pushHost the base of the url for push requests
     * @param userSchema the name of your app's user object. if you do not have a user object, pass the empty strinrg here and do not use the login, logout, facebook or twitter methods, as they will fail
     * @param userIdName the name of your app's user object primary key
     * @param passwordFieldName the name of your app's user object primary key
     * @param redirectedCallback callback to be called if the StackMob platform issues a redirect. you should use this callback to cache the new URLs. here is a sample callback:
     * <code>
     * new StackMobRedirectedCallback() {
     *   public void redirected(HttpRequest origRequest, HttpResponse response, HttpRequest newRequest) {
     *       try {
     *           URI uri = new URI(newRequest.getRequestLine().getUri());
     *           cache(uri.getHost);
     *       }
     *        catch (URISyntaxException e) {
     *           handleException(e);
     *       }
     *   }
     * }
     * }
     * </code>
     * note that this callback may be called in a background thread
     */
    public StackMob(OAuthVersion oauthVersion,
                    Integer apiVersionNumber,
                    String apiKey,
                    String apiSecret,
                    String apiHost,
                    String pushHost,
                    String userSchema,
                    String userIdName,
                    String passwordFieldName,
                    StackMobRedirectedCallback redirectedCallback) {
        this.session = new StackMobSession(oauthVersion, apiVersionNumber, apiKey, apiSecret, userSchema, userIdName);
        this.executor = createNewExecutor();
        if(stackmob == null) StackMob.setStackMob(this);
        this.apiUrlFormat = apiHost;
        this.pushUrlFormat = pushHost;
        this.userIdName = userIdName;
        this.passwordField = passwordFieldName;
        this.userRedirectedCallback = redirectedCallback;
        this.push = new StackMobPush(executor, session, pushHost, redirectedCallback);
        this.datastore = new StackMobDatastore(executor, session, apiHost, redirectedCallback);
    }

    /**
     * Copy constructor
     * @param other the StackMob to copy
     */
    public StackMob(StackMob other) {
        this.session = other.session;
        this.userRedirectedCallback = other.redirectedCallback;
        this.apiUrlFormat = other.apiUrlFormat;
        this.pushUrlFormat = other.pushUrlFormat;
        this.executor = other.executor;
    }

    private StackMobPush push;

    public StackMobPush getPush() {
        return push;
    }

    private StackMobDatastore datastore;

    public StackMobDatastore getDatastore() {
       return datastore;
    }

    ////////////////////
    //session & login/logout
    ////////////////////

    /**
     * call the login method on StackMob
     * @param params parameters to pass to the login method
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void login(Map<String, String> params, StackMobRawCallback callback) {
        StackMobRequest req;
        if(getSession().isOAuth2()) {
            Map<String, String> newParams = new HashMap<String, String>(params);
            newParams.put("token_type", "mac");
            newParams.put("mac_algorithm", "hmac-sha-1");
            req = new StackMobAccessTokenRequest(this.executor,
                                                 this.session,
                                                 "accessToken",
                                                 newParams,
                                                 callback,
                                                 this.redirectedCallback);
        } else {
            req = new StackMobUserBasedRequest(this.executor,
                                               this.session,
                                               "login",
                                               params,
                                               callback,
                                               this.redirectedCallback);
        }
        req.setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Refresh the current OAuth2 login. This ordinarily happens automatically, but this method
     * can give you finer control if you need it. Logins last an hour by default. Once they expire
     * they need to be refreshed. Make sure not to send multiple refresh token requests.
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void refreshLogin(StackMobRawCallback callback) {
        if(!getSession().isOAuth2()) {
            callback.unsent(new StackMobException("This method is only available with oauth2"));
        }

        if(!getSession().oauth2RefreshTokenValid()) {
            callback.unsent(new StackMobException("Refresh token invalid"));
        }
        StackMobAccessTokenRequest.newRefreshTokenRequest(executor, session, this.redirectedCallback, callback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * call the logout method on StackMob, invalidating your credentials.
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void logout(StackMobRawCallback callback) {
        session.setOAuth2TokensAndExpiration(null, null, null, 0);
        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "logout",
                                     StackMobRequest.EmptyParams,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    ////////////////////
    //social
    ////////////////////

    /**
     * login to StackMob with twitter credentials. The credentials should match a existing user object that has a linked Twitter
     * account, via either {@link #registerWithTwitterToken(String, String, String, com.stackmob.sdk.callback.StackMobRawCallback)} or
     * {@link #linkUserWithTwitterToken(String, String, com.stackmob.sdk.callback.StackMobRawCallback)}
     * @param token the twitter session key (this is a per user key - different from the consumer key)
     * @param secret the twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void twitterLogin(String token,
                                                  String secret,
                                                  StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("tw_tk", token);
        params.put("tw_ts", secret);

        StackMobRequest req;
        if(getSession().isOAuth2()) {
            req = new StackMobAccessTokenRequest(this.executor,
                    this.session,
                    "twitterAccessToken",
                    params,
                    callback,
                    this.redirectedCallback);
        } else {
            req = new StackMobUserBasedRequest(this.executor,
                    this.session,
                    "twitterlogin",
                    params,
                    callback,
                    this.redirectedCallback);
        }
        req.setUrlFormat(this.apiUrlFormat).sendRequest();

    }

    /**
     * update the logged in users’s Twitter status. The logged in user must have a linked Twitter account.
     * @param message the message to send. must be <= 140 characters
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void twitterStatusUpdate(String message,
                                                         StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("tw_st", message);
        new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            "twitterStatusUpdate",
                                            params,
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * create a new user on StackMob and associate it with an existing Twitter user via Twitter credentials.
     * @param token the twitter session key (this is a per user key - different from the consumer key)
     * @param secret the twitter session secret (this is a per user secret - different from the consumer secret)
     * @param username the username that the user should have
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void registerWithTwitterToken(String token,
                                                              String secret,
                                                              String username,
                                                              StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("tw_tk", token);
        params.put("tw_ts", secret);
        params.put("username", username);
        new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            "createUserWithTwitter",
                                            params,
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * link an existing StackMob user with an existing Twitter user via Twitter credentials.
     * @param token the twitter session key (this is a per user key - different from the consumer key)
     * @param secret the twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void linkUserWithTwitterToken(String token,
                                         String secret,
                                         StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("tw_tk", token);
        params.put("tw_ts", secret);

        new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            "linkUserWithTwitter",
                                            params,
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * login to StackMob with Facebook credentials. The credentials should match a existing user object that has a linked Facebook
     * account, via either {@link #registerWithFacebookToken(String, String, com.stackmob.sdk.callback.StackMobRawCallback)} or
     * {@link #linkUserWithFacebookToken(String, com.stackmob.sdk.callback.StackMobRawCallback)}
     * @param token the facebook user token
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void facebookLogin(String token,
                                                   StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("fb_at", token);


        StackMobRequest req;
        if(getSession().isOAuth2()) {
            req = new StackMobAccessTokenRequest(this.executor,
                                                 this.session,
                                                 "facebookAccessToken",
                                                 params,
                                                 callback,
                                                 this.redirectedCallback);
        } else {
            req = new StackMobUserBasedRequest(this.executor,
                                               this.session,
                                               "facebookLogin",
                                               params,
                                               callback,
                                               this.redirectedCallback);
        }
        req.setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * create a new user on StackMob and associate it with an existing Facebook user via Facebook credentials.
     * @param token the facebook user token
     * @param username the StackMob username that the new user should have
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void registerWithFacebookToken(String token,
                                                               String username,
                                                               StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("fb_at", token);
        params.put("username", username);

        new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            "createUserWithFacebook",
                                            params,
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * link an existing StackMob user with an existing Facebook user via Facebook credentials.
     * @param token the Facebook user token
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void linkUserWithFacebookToken(String token,
                                                               StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("fb_at", token);

        new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            "linkUserWithFacebook",
                                            params,
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * post a message to Facebook. This method will not post to FB and will return nothing if there is no user logged into FB.
     * @param msg the message to post
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void facebookPostMessage(String msg,
                                                         StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("message", msg);

        new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            "postFacebookMessage",
                                            params,
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * get facebook user info for the current user. this method will return nothing if there is no currently logged in FB user
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void getFacebookUserInfo(StackMobRawCallback callback) {
        new StackMobUserBasedRequest(this.executor, this.session, "getFacebookUserInfo", new HashMap<String, String>(), callback, this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * get twitter user info for the current user. this method will return nothing if there is no currently logged in twitter user
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void getTwitterUserInfo(StackMobRawCallback callback) {
        new StackMobUserBasedRequest(this.executor, this.session, "getTwitterUserInfo", new HashMap<String, String>(), callback, this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    //Forgot/reset password

    /**
     * send out a password reset email to a user who's forgotten their password
     * @param username the user who's forgotten their password
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */

    public void forgotPassword(String username,
                                                   StackMobRawCallback callback) {

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            HttpVerbWithPayload.POST,
                                            StackMobRequest.EmptyHeaders,
                                            StackMobRequest.EmptyParams,
                                            params,
                                            "forgotPassword",
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * reset the logged in user's password
     * @param oldPassword the old temporary password
     * @param newPassword the new password that the user just created
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */

    public void resetPassword(String oldPassword,
                                                   String newPassword,
                                                   StackMobRawCallback callback) {

        Map<String, Map<String, String>> params = new HashMap<String, Map<String, String>>();
        Map<String, String> oldPW = new HashMap<String, String>();
        oldPW.put("password", oldPassword);
        Map<String, String> newPW = new HashMap<String, String>();
        newPW.put("password", newPassword);
        params.put("old", oldPW);
        params.put("new", newPW);
        new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            HttpVerbWithPayload.POST,
                                            StackMobRequest.EmptyHeaders,
                                            StackMobRequest.EmptyParams,
                                            params,
                                            "resetPassword",
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Gets the user object for the currently logged in oauth2 user. Invokes the failure callback if there
     * is no logged in user
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void getLoggedInUser(StackMobCallback callback) {
        datastore.get("user/loggedInUser", callback);
    }

    /**
     * get the logged in user locally. This method is deprecated and {@link #getLoggedInUser(com.stackmob.sdk.callback.StackMobCallback)} should be
     * use instead
     * @return the logged in user
     */
    public String getLoggedInUsername() {
        return isLoggedIn() ? session.getLastUserLoginName() : null;
    }

    /**
     * check whether a user is currently logged in. In rare cases when a user is logged off remotely this may be inaccurate
     * @return whether the user is logged in
     */
    public boolean isLoggedIn() {
        if(getSession().isOAuth2()) {
            return getSession().oauth2TokenValid();
        } else {
            Map.Entry<String, Date> sessionCookie = StackMobRequest.getCookieStore().getSessionCookie();
            if(sessionCookie != null) {
                boolean cookieIsStillValid = sessionCookie.getValue() == null || sessionCookie.getValue().before(new Date());
                return cookieIsStillValid && !this.isLoggedOut();
            }
        }
        return false;
    }

    /**
     * check if a specific user is logged in. Use {@link #getLoggedInUser(com.stackmob.sdk.callback.StackMobCallback)} instead
     * @param username the user to check
     * @return whether that user is logged in
     */
    public boolean isUserLoggedIn(String username) {
        return username != null && username.equals(this.getLoggedInUsername());
    }

    /**
     * check whether the user is logged out.
     * @return whether the user is logged out
     */
    public boolean isLoggedOut() {
        if(getSession().isOAuth2()) {
            return getSession().getOAuth2TokenExpiration() != null && !getSession().oauth2TokenValid();
        } else {
            Map.Entry<String, Date> sessionCookie = StackMobRequest.getCookieStore().getSessionCookie();
            //The logged out cookie is a json string.
            return sessionCookie != null && sessionCookie.getKey().contains(":");
        }
    }

    /**
     * get the session that this StackMob object contains
     * @return the session
     */
    public StackMobSession getSession() {
        return session;
    }

    /**
     * set a specific session
     * @param session the session to set
     */
    public void setSession(StackMobSession session) {
        this.session = session;
    }

    /**
     * get the OAuthVersion this StackMob object is configured for
     * @return the current OAuth version
     */
    public OAuthVersion getOAuthVersion() {
        return session.getOAuthVersion();
    }
}
