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

import com.stackmob.sdk.callback.*;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.net.HttpVerbWithPayload;
import com.stackmob.sdk.request.*;
import com.stackmob.sdk.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The StackMob object is your interface for accessing StackMob's many features. Its functions include:
 * <ul>
 *  <li>Logging in and managing user sessions</li>
 *  <li>Datastore API Methods</li>
 *  <li>Push API Requests</li>
 *  <li>Social Network API Requests</li>
 *  </ul>
 * <p>
 * A StackMob instance is created with authorization credentials along with some optional configuration
 * parameters. To use different configurations in one app, simply instantiate multiple StackMob objects.
 */
public class StackMob {

    /**
     * The two different OAuth versions the SDK can use for authentication. The Push API currently only supports
     * OAuth1 and will use that regardless of which OAuth version you're using.
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
    private String userSchema;
    private String userIdName;
    private String passwordField;
    private String apiUrlFormat;
    private String pushUrlFormat;
    private ExecutorService executor;
    private StackMobDatastore datastore;

    private final Object urlFormatLock = new Object();

    private static final String versionKey= "sdk.version";
    private static String version = null;

    public static String DEFAULT_API_HOST = "api.stackmob.com";
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
            InputStream buildProps = null;
            try {
                buildProps = StackMob.class.getClassLoader().getResourceAsStream("build.properties");
                props.load(buildProps);
            } catch (IOException e) {
            } catch (NullPointerException e) {
            } finally {
                try { buildProps.close(); } catch (Exception ignore) { }
            }
            if( props.containsKey(versionKey) && props.get(versionKey) != null) {
                version = props.getProperty(versionKey);
                //This should be replaced by a real version in maven builds
                if("${version}".equals(version)) version = "dev";
            }
        }
        return version;
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
     * A StackMob constructor allowing you to specify the OAuth version.
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session.
     *                         Pass 0 for sandbox.
     * @param apiKey the api key for your app
     */
    public StackMob(Integer apiVersionNumber,
                    String apiKey) {
        this(OAuthVersion.Two, apiVersionNumber, apiKey, null);
    }

    /**
     * A StackMob constructor allowing you to specify the OAuth version.
     * @param oauthVersion whether to use OAuth1 or OAuth2
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session.
     *                         Pass 0 for sandbox.
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     */
    public StackMob(OAuthVersion oauthVersion,
                    Integer apiVersionNumber,
                    String apiKey,
                    String apiSecret) {
        this(oauthVersion, apiVersionNumber, apiKey, apiSecret, DEFAULT_API_HOST, DEFAULT_USER_SCHEMA_NAME,
                DEFAULT_USER_ID, DEFAULT_PASSWORD_FIELD, DEFAULT_REDIRECTED_CALLBACK);
    }

    /**
     * The most complete StackMob constructor, allowing you to set values for everything.
     * @param oauthVersion whether to use OAuth1 or OAuth2
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session.
     *                         Pass 0 for sandbox.
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app. Can be null if you're using OAuth2.
     * @param apiHost the base of the url for api requests
     * @param userSchema the name of your app's user object. If you do not have a user object, pass the empty string
     *                   here, and do not use the login, logout, Facebook or Twitter methods, as they will fail.
     * @param userIdName the name of your app's user object primary key
     * @param passwordFieldName the name of your app's user object primary key
     * @param redirectedCallback callback to be called if the StackMob platform issues a redirect. You should use this
     *                           callback to cache the new URLs. Here is a sample callback:
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
     * </code>
     * Note that this callback may be called in a background thread.
     */
    public StackMob(OAuthVersion oauthVersion,
                    Integer apiVersionNumber,
                    String apiKey,
                    String apiSecret,
                    String apiHost,
                    String userSchema,
                    String userIdName,
                    String passwordFieldName,
                    StackMobRedirectedCallback redirectedCallback) {
        this.session = new StackMobSession(oauthVersion, apiVersionNumber, apiKey, apiSecret, userSchema, userIdName);
        this.executor = createNewExecutor();
        this.apiUrlFormat = apiHost;
        this.userSchema = userSchema;
        this.userIdName = userIdName;
        this.passwordField = passwordFieldName;
        this.userRedirectedCallback = redirectedCallback;
        this.datastore = new StackMobDatastore(executor, session, apiHost, redirectedCallback);
        if(stackmob == null) StackMob.setStackMob(this);
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

    /**
     * Access Datastore API methods
     * @return a StackMobDatastore instance with the same credentials
     */
    public StackMobDatastore getDatastore() {
       return datastore;
    }

    // ================================================================================================================
    // Session & login/logout

    /**
     * Call the login method on StackMob.
     * @param params parameters to pass to the login method
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void login(Map<String, String> params,
                      StackMobRawCallback callback) {
        login(params, new StackMobOptions(), callback);
    }

    /**
     * Call the login method on StackMob.
     * @param params parameters to pass to the login method
     * @param options additional options, such as headers, to modify the request
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void login(Map<String, String> params,
                      StackMobOptions options,
                      StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>(params.entrySet());
        StackMobRequest req;
        if(getSession().isOAuth2()) {
            req = new StackMobAccessTokenRequest(this.executor,
                    this.session,
                    "accessToken",
                    options,
                    paramList,
                    callback,
                    this.redirectedCallback);
        } else {
            session.setLastUserLoginName(params.get(userIdName));
            req = new StackMobUserBasedRequest(this.executor,
                    this.session,
                    "login",
                    paramList,
                    callback,
                    this.redirectedCallback);
        }
        req.setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Refresh the current OAuth2 login. This normally happens automatically, but this method
     * can give you finer control if you need it. Logins last an hour by default. Once they expire,
     * they need to be refreshed. Make sure not to send multiple refresh token requests.
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void refreshLogin(StackMobRawCallback callback) {
        if(!getSession().isOAuth2()) {
            callback.unsent(new StackMobException("This method is only available with OAuth2"));
        } else if(!getSession().oauth2RefreshTokenValid()) {
            callback.unsent(new StackMobException("Refresh token invalid"));
        } else {
            StackMobAccessTokenRequest.newRefreshTokenRequest(executor, session, this.redirectedCallback, callback)
                    .setUrlFormat(this.apiUrlFormat).sendRequest();
        }
    }

    /**
     * Call the logout method on StackMob, invalidating the current user's credentials.
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void logout(StackMobRawCallback callback) {
        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "logout",
                                     StackMobRequest.EmptyParams,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
        session.setOAuth2TokensAndExpiration(null, null, null, 0);
    }

    // ================================================================================================================
    // Social API Integration

    /**
     * Login to StackMob with Twitter credentials. The credentials should match an existing StackMob user object with a
     * linked Twitter account, via either
     * {@link #registerWithTwitterToken(String, String, String, com.stackmob.sdk.callback.StackMobRawCallback)} or
     * {@link #linkUserWithTwitterToken(String, String, com.stackmob.sdk.callback.StackMobRawCallback)}.
     * @param token the Twitter session key (this is a per user key - different from the consumer key)
     * @param secret the Twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void twitterLogin(String token,
                             String secret,
                             StackMobRawCallback callback) {
        twitterLogin(token, secret, new StackMobOptions(), callback);
    }

    /**
     * Login to StackMob with Twitter credentials. The credentials should match an existing StackMob user object with a
     * linked Twitter account, via either
     * {@link #registerWithTwitterToken(String, String, String, com.stackmob.sdk.callback.StackMobRawCallback)} or
     * {@link #linkUserWithTwitterToken(String, String, com.stackmob.sdk.callback.StackMobRawCallback)}.
     * @param token the Twitter session key (this is a per user key - different from the consumer key)
     * @param secret the Twitter session secret (this is a per user secret - different from the consumer secret)
     * @param options additional options, such as headers, to modify the request
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void twitterLogin(String token,
                             String secret,
                             StackMobOptions options,
                             StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("tw_tk", token));
        paramList.add(new Pair<String, String>("tw_ts", secret));

        StackMobRequest req;
        if(getSession().isOAuth2()) {
            req = new StackMobAccessTokenRequest(this.executor,
                    this.session,
                    "twitterAccessToken",
                    options,
                    paramList,
                    callback,
                    this.redirectedCallback);
        } else {
            req = new StackMobUserBasedRequest(this.executor,
                    this.session,
                    "twitterlogin",
                    paramList,
                    callback,
                    this.redirectedCallback);
        }
        req.setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Update the logged-in user’s Twitter status. The logged-in user must be linked with a Twitter account.
     * @param message the message to send. must be <= 140 characters
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void twitterStatusUpdate(String message, StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("tw_st", message));
        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "twitterStatusUpdate",
                                     paramList,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Create a new user on StackMob and associate it with an existing Twitter user via Twitter credentials.
     * @param token the Twitter session key (this is a per-user key, different from the app's consumer key)
     * @param secret the Twitter session secret (this is a per-user secret, different from the app's consumer secret)
     * @param username the username that the StackMob user should have
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void registerWithTwitterToken(String token,
                                         String secret,
                                         String username,
                                         StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("tw_tk", token));
        paramList.add(new Pair<String, String>("tw_ts", secret));
        if(username != null) paramList.add(new Pair<String, String>("username", username));
        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "createUserWithTwitter",
                                     paramList,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Link an existing StackMob user with an existing Twitter user via Twitter credentials.
     * @param token the Twitter session key (this is a per-user key, different from the app's consumer key)
     * @param secret the Twitter session secret (this is a per-user secret, different from the app's consumer secret)
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void linkUserWithTwitterToken(String token,
                                         String secret,
                                         StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("tw_tk", token));
        paramList.add(new Pair<String, String>("tw_ts", secret));

        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "linkUserWithTwitter",
                                     paramList,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Login to StackMob with Facebook credentials. The credentials should match a existing user object that has a
     * linked Facebook account, via either
     * {@link #registerWithFacebookToken(String, String, com.stackmob.sdk.callback.StackMobRawCallback)} or
     * {@link #linkUserWithFacebookToken(String, com.stackmob.sdk.callback.StackMobRawCallback)}.
     * @param token the Facebook user token
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void facebookLogin(String token,
                              StackMobRawCallback callback) {
        facebookLogin(token, new StackMobOptions(), callback);
    }

    /**
     * Login to StackMob with Facebook credentials. The credentials should match a existing user object that has a
     * linked Facebook account, via either
     * {@link #registerWithFacebookToken(String, String, com.stackmob.sdk.callback.StackMobRawCallback)} or
     * {@link #linkUserWithFacebookToken(String, com.stackmob.sdk.callback.StackMobRawCallback)}.
     * @param token the Facebook user token
     * @param options additional options, such as headers, to modify the request
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void facebookLogin(String token,
                              StackMobOptions options,
                              StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("fb_at", token));

        StackMobRequest req;
        if(getSession().isOAuth2()) {
            req = new StackMobAccessTokenRequest(this.executor,
                                                 this.session,
                                                 "facebookAccessToken",
                                                 options,
                                                 paramList,
                                                 callback,
                                                 this.redirectedCallback);
        } else {
            req = new StackMobUserBasedRequest(this.executor,
                                               this.session,
                                               "facebookLogin",
                                               paramList,
                                               callback,
                                               this.redirectedCallback);
        }
        req.setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Create a new user on StackMob and associate it with an existing Facebook user via Facebook credentials.
     * @param token the Facebook user token
     * @param username the StackMob username that the new user should have
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void registerWithFacebookToken(String token,
                                          String username,
                                          StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("fb_at", token));
        paramList.add(new Pair<String, String>("username", username));

        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "createUserWithFacebook",
                                     paramList,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Link an existing StackMob user with an existing Facebook user via Facebook credentials.
     * @param token the Facebook user token
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void linkUserWithFacebookToken(String token,
                                          StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("fb_at", token));

        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "linkUserWithFacebook",
                                     paramList,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Post a message to Facebook. If there is no user logged into Facebook, this method will not post to Facebook and
     * will return nothing.
     * @param msg the message to post
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void facebookPostMessage(String msg,
                                    StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("message", msg));

        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "postFacebookMessage",
                                     paramList,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Login to StackMob with Gigya credentials. If a corresponding StackMob user didn't exist before, it will
     * be created.
     * @param gigyaUid The parameter UID
     * @param timestamp The parameter signatureTimestamp
     * @param sig The parameter UIDSignature
     * @param options additional options, such as headers, to modify the request
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void gigyaLogin(String gigyaUid,
                           String timestamp,
                           String sig,
                           StackMobOptions options,
                           StackMobRawCallback callback) {
        List<Map.Entry<String, String>> paramList = new LinkedList<Map.Entry<String, String>>();
        paramList.add(new Pair<String, String>("gigya_uid", gigyaUid));
        paramList.add(new Pair<String, String>("gigya_ts", timestamp));
        paramList.add(new Pair<String, String>("gigya_sig", sig));

        StackMobRequest req;
        if(getSession().isOAuth2()) {
            req = new StackMobAccessTokenRequest(this.executor,
                    this.session,
                    "gigyaAccessToken",
                    options,
                    paramList,
                    callback,
                    this.redirectedCallback);
        } else {
            throw new UnsupportedOperationException("Gigya login is only supported for OAuth2");
        }
        req.setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Get Facebook user info for the current user. This method will return nothing if there is no currently logged-in
     * Facebook user
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void getFacebookUserInfo(StackMobRawCallback callback) {
        new StackMobUserBasedRequest(this.executor, this.session, "getFacebookUserInfo", StackMobRequest.EmptyParams,
                callback, this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Get Twitter user info for the current user. This method will return nothing if there is no currently logged-in
     * Twitter user
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void getTwitterUserInfo(StackMobRawCallback callback) {
        new StackMobUserBasedRequest(this.executor, this.session, "getTwitterUserInfo", StackMobRequest.EmptyParams,
                callback, this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    // ================================================================================================================
    // Forgot/reset password

    /**
     * Send out a password reset email to a user who has forgotten their password.
     * @param username the user who's forgotten their password
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void forgotPassword(String username,
                               StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     HttpVerbWithPayload.POST,
                                     StackMobOptions.none(),
                                     StackMobRequest.EmptyParams,
                                     params,
                                     "forgotPassword",
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Reset the logged-in user's password
     * @param oldPassword the old temporary password
     * @param newPassword the new password that the user just created
     * @param callback callback to be called when the server returns. May execute in a separate thread.
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
                                     StackMobOptions.none(),
                                     StackMobRequest.EmptyParams,
                                     params,
                                     "resetPassword",
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    // ================================================================================================================
    // Additional getter/setter methods

    /**
     * Gets the user object for the currently logged-in OAuth2 user. Invokes the failure callback if there
     * is no logged-in user
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void getLoggedInUser(StackMobCallback callback) {
        datastore.get(userSchema + "/loggedInUser", callback);
    }

    /**
     * Gets the user object for the currently logged-in OAuth2 user. Invokes the failure callback if there
     * is no logged-in user
     * @param options additional options, such as headers, to modify the request
     * @param callback callback to be called when the server returns. May execute in a separate thread.
     */
    public void getLoggedInUser(StackMobOptions options,
                                StackMobCallback callback) {
        datastore.get(userSchema + "/loggedInUser", options, callback);
    }

    /**
     * Get the logged-in user's username, or null if there is no logged-in user.
     * This method is deprecated and {@link #getLoggedInUser(com.stackmob.sdk.callback.StackMobCallback)} should be
     * used instead.
     * @return the logged-in user
     */
    @Deprecated
    public String getLoggedInUsername() {
        return isLoggedIn() ? session.getLastUserLoginName() : null;
    }

    /**
     * Check whether a user is currently logged in. In rare cases when a user is logged off remotely, this may be
     * inaccurate.
     * @return whether the user is logged in
     */
    public boolean isLoggedIn() {
        if(getSession().isOAuth2()) {
            return getSession().oauth2RefreshTokenValid();
        } else {
            Map.Entry<String, Date> sessionCookie = session.getCookieManager().getSessionCookie();
            if(sessionCookie != null) {
                boolean cookieIsStillValid =
                        sessionCookie.getValue() == null || sessionCookie.getValue().before(new Date());
                return cookieIsStillValid && !this.isLoggedOut();
            }
        }
        return false;
    }


    /**
     * Check whether a {@link #refreshLogin(com.stackmob.sdk.callback.StackMobRawCallback)} call is required
     * to continue making authenticated requests. This will happen automatically, so there's no reason to
     * check this method unless you're overriding the existing refresh token system. If there are no credentials
     * at all, this will be false.
     *
     * @return whether there's a valid refresh token that can be used to refresh the login
     */
    public boolean refreshRequired() {
        return getSession().isOAuth2() && !getSession().oauth2TokenValid();
    }

    /**
     * Check if a specific user is logged in. Use {@link #getLoggedInUser(com.stackmob.sdk.callback.StackMobCallback)}
     * instead.
     * @param username the user to check
     * @return whether that user is logged in
     */
    public boolean isUserLoggedIn(String username) {
        return username != null && username.equals(this.getLoggedInUsername());
    }

    /**
     * Check whether the current user is logged out.
     * @return whether the current user is logged out
     */
    public boolean isLoggedOut() {
        if(getSession().isOAuth2()) {
            return getSession().getOAuth2TokenExpiration() != null && !getSession().oauth2TokenValid();
        } else {
            Map.Entry<String, Date> sessionCookie = session.getCookieManager().getSessionCookie();
            //The logged out cookie is a json string.
            return sessionCookie != null && sessionCookie.getKey().contains(":");
        }
    }

    /**
     * Get the session contained by this StackMob object.
     * @return the session
     */
    public StackMobSession getSession() {
        return session;
    }

    /**
     * Set a specific session.
     * @param session the session to set
     */
    public void setSession(StackMobSession session) {
        this.session = session;
        this.datastore.setSession(session);
    }


    /**
     * Get the executor used for requests.
     * @return the executor
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Get the callback used for redirected requests.
     * @return the redirected callback
     */
    public StackMobRedirectedCallback getRedirectedCallback() {
        return userRedirectedCallback;
    }

    /**
     * Get the OAuthVersion that is set for this StackMob object.
     * @return the current OAuth version
     */
    public OAuthVersion getOAuthVersion() {
        return session.getOAuthVersion();
    }

    /**
     * Get the primary key for this user object.
     * @return the user id name
     */
    public String getUserIdName() {
        return userIdName;
    }

    /**
     * Get the name of the password field in the user object.
     * @return the password field
     */
    public String getPasswordField() {
        return passwordField;
    }
}

