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
import com.stackmob.sdk.push.StackMobPushToken;
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

    /**
     * Describes the two different Android Push systems the sdk can use.
     */
    public static enum PushType {
        /**
         * The old, deprecated Android Push, included for legacy reasons.
         */
        C2DM,
        /**
         * The new Android Push, used by default.
         */
        GCM
    }

    private StackMobSession session;
    private String apiUrlFormat = StackMobRequest.DEFAULT_API_URL_FORMAT;
    private String pushUrlFormat = StackMobRequest.DEFAULT_PUSH_URL_FORMAT;
    private ExecutorService executor;
    private PushType defaultPushType = PushType.GCM;

    private final Object urlFormatLock = new Object();

    private static class RegistrationIDAndUser {

        public String userId;
        public Map<String, String> token = new HashMap<String, String>();
        public Boolean overwrite = null;

        public RegistrationIDAndUser(String registrationID, String user) {
            this(registrationID, user, StackMobPushToken.TokenType.Android);
        }

        public RegistrationIDAndUser(String registrationID, String user, StackMobPushToken.TokenType platform) {
            userId = user;
            token.put("token", registrationID);
            token.put("type", platform.toString());
        }
        public RegistrationIDAndUser(String registrationID, String user, StackMobPushToken.TokenType platform, boolean overwrite) {
            this(registrationID, user, platform);
            this.overwrite = overwrite;
        }

        public RegistrationIDAndUser(String registrationID, String user, PushType type, boolean overwrite) {
            this(registrationID, user, type == PushType.C2DM ? StackMobPushToken.TokenType.AndroidC2DM : StackMobPushToken.TokenType.Android);
            this.overwrite = overwrite;
        }

        public RegistrationIDAndUser(String registrationID, String user, boolean overwrite) {
            this(registrationID, user, StackMobPushToken.TokenType.Android, overwrite);
        }
    }

    private static final String versionKey= "sdk.version";
    private static String userAgentName = "Java Client";
    private static String version = null;
    private static StackMobLogger logger = new StackMobLogger();

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

    protected static String getUserAgent(String appName) {
        return String.format("StackMob (%s; %s)%s", userAgentName,
                getVersion(),
                (appName == null) ? "" : "/" + appName);
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
     * Get the singleton StackMob object. If none exists, one is created from the values in {@link StackMobConfiguration}
     * @return the singleton StackMob
     */
    public static synchronized StackMob getStackMob() {
        if(stackmob == null) {
            stackmob = StackMobConfiguration.newStackMob();
        }
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
     * the preferred StackMob constructor. Unspecified parameters get reasonable defaults
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     * @param userObjectName the name of your app's user object. if you do not have a user object, pass the empty strinrg here and do not use the login, logout, facebook or twitter methods, as they will fail
     * @param appName the name of your application
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     */
    public StackMob(String apiKey, String apiSecret, String userObjectName, String appName, Integer apiVersionNumber) {
        this(OAuthVersion.One, apiKey, apiSecret, userObjectName, appName, apiVersionNumber);
    }

    /**
     * a StackMob constructor allowing you to specify the OAuth version.
     * @param oauthVersion whether to use oauth1 or oauth2
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     * @param userObjectName the name of your app's user object. if you do not have a user object, pass the empty strinrg here and do not use the login, logout, facebook or twitter methods, as they will fail
     * @param appName the name of your application
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     */
    public StackMob(OAuthVersion oauthVersion, String apiKey, String apiSecret, String userObjectName, String appName, Integer apiVersionNumber) {
        this.session = new StackMobSession(oauthVersion, apiKey, apiSecret, userObjectName, appName, apiVersionNumber);
        this.executor = createNewExecutor();
        if(stackmob == null) StackMob.setStackMob(this);
    }

    /**
    * a StackMob constructor that leaves you with now app name in the user-agent
    * @param apiKey the api key for your app
    * @param apiSecret the api secret for your app
    * @param userObjectName the name of your app's user object. if you do not have a user object, pass the empty string here and do not use the login, logout, facebook or twitter methods, as they will fail
    * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
    */
    public StackMob(String apiKey, String apiSecret, String userObjectName, Integer apiVersionNumber) {
        this(apiKey, apiSecret, userObjectName,  null, apiVersionNumber);
    }

    /**
     * a StackMob constructor that allows you to do your own caching of URLs for redirection
     * @param oauthVersion whether to use oauth1 or oauth2
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     * @param userObjectName the name of your app's user object
     * @param apiVersionNumber the version number of your app's API that you want to use with this object. pass 0 for sandbox
     * @param urlFormat the format of URLs to use. for instance: api.mob1.stackmob.com
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
                    String apiKey,
                    String apiSecret,
                    String userObjectName,
                    Integer apiVersionNumber,
                    String urlFormat,
                    StackMobRedirectedCallback redirectedCallback) {
        this(oauthVersion, apiKey, apiSecret, userObjectName, null, apiVersionNumber, urlFormat, StackMobRequest.DEFAULT_PUSH_URL_FORMAT, redirectedCallback);
    }

    /**
     * a legacy constructor with a subset of the possible parameters
     * @deprecated
     * @param oauthVersion whether to use oauth1 or oauth2
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     * @param userObjectName the name of your app's user object. if you do not have a user object, pass the empty strinrg here and do not use the login, logout, facebook or twitter methods, as they will fail
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     * @param apiUrlFormat the url for api requests
     * @param pushUrlFormat the url for push requests
     * @param redirectedCallback a calback to be called on redirection
     */
    public StackMob(OAuthVersion oauthVersion,
                    String apiKey,
                    String apiSecret,
                    String userObjectName,
                    Integer apiVersionNumber,
                    String apiUrlFormat,
                    String pushUrlFormat,
                    StackMobRedirectedCallback redirectedCallback) {
        this(oauthVersion, apiKey, apiSecret, userObjectName, null, apiVersionNumber, apiUrlFormat, pushUrlFormat, redirectedCallback);
    }

    /**
     * a legacy constructor with a subset of the possible parameters
     * @deprecated
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     * @param userObjectName the name of your app's user object. if you do not have a user object, pass the empty strinrg here and do not use the login, logout, facebook or twitter methods, as they will fail
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     * @param apiUrlFormat the url for api requests
     * @param pushUrlFormat the url for push requests
     * @param redirectedCallback a calback to be called on redirection
     */
    public StackMob(String apiKey,
                    String apiSecret,
                    String userObjectName,
                    Integer apiVersionNumber,
                    String apiUrlFormat,
                    String pushUrlFormat,
                    StackMobRedirectedCallback redirectedCallback) {
        this(OAuthVersion.One, apiKey, apiSecret, userObjectName, null, apiVersionNumber, apiUrlFormat, pushUrlFormat, redirectedCallback);
    }

    /**
     * a legacy constructor with a subset of the possible parameters
     * @deprecated
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     * @param userObjectName the name of your app's user object. if you do not have a user object, pass the empty strinrg here and do not use the login, logout, facebook or twitter methods, as they will fail
     * @param appName the name of your application
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     * @param apiUrlFormat the url for api requests
     * @param pushUrlFormat the url for push requests
     * @param redirectedCallback a calback to be called on redirection
     */
    public StackMob(String apiKey,
                    String apiSecret,
                    String userObjectName,
                    String appName,
                    Integer apiVersionNumber,
                    String apiUrlFormat,
                    String pushUrlFormat,
                    StackMobRedirectedCallback redirectedCallback) {
        this(OAuthVersion.One, apiKey,apiSecret, userObjectName, appName, apiVersionNumber, apiUrlFormat, pushUrlFormat, redirectedCallback);
    }

    /**
     * the most complete StackMob constructor allowing you to set values for everything
     * @param oauthVersion whether to use oauth1 or oauth2
     * @param apiKey the api key for your app
     * @param apiSecret the api secret for your app
     * @param userObjectName the name of your app's user object. if you do not have a user object, pass the empty strinrg here and do not use the login, logout, facebook or twitter methods, as they will fail
     * @param appName the name of your application
     * @param apiVersionNumber the version of your app's API that you want to use with this StackMob session. pass 0 for sandbox
     * @param apiUrlFormat the url for api requests
     * @param pushUrlFormat the url for push requests
     * @param redirectedCallback a calback to be called on redirection
     */
    public StackMob(OAuthVersion oauthVersion,
                    String apiKey,
                    String apiSecret,
                    String userObjectName,
                    String appName,
                    Integer apiVersionNumber,
                    String apiUrlFormat,
                    String pushUrlFormat,
                    StackMobRedirectedCallback redirectedCallback) {
        this(oauthVersion, apiKey, apiSecret, userObjectName, appName, apiVersionNumber);
        this.userRedirectedCallback = redirectedCallback;
        this.apiUrlFormat = apiUrlFormat;
        this.pushUrlFormat = pushUrlFormat;
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
     * Sets the type of push this StackMob instance will do. The default is
     * GCM. Use this to switch back to C2DM if you need to
     * @param type C2DM or GCM
     */
    public void setPushType(PushType type) {
        this.defaultPushType = type;
    }

    ////////////////////
    //session & login/logout
    ////////////////////

    /**
     * call the login method on StackMob
     * @param params parameters to pass to the login method
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult login(Map<String, String> params, StackMobRawCallback callback) {
        session.setLastUserLoginName(params.get("username"));
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
        return req.setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * Refresh the current OAuth2 login. This ordinarily happens automatically, but this method
     * can give you finer control if you need it. Logins last an hour by default. Once they expire
     * they need to be refreshed. Make sure not to send multiple refresh token requests.
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult refreshToken(StackMobRawCallback callback) {
        if(!getSession().isOAuth2()) {
            return new StackMobRequestSendResult(StackMobRequestSendResult.RequestSendStatus.FAILED, new StackMobException("This method is only available with oauth2"));
        }

        if(!getSession().oauth2RefreshTokenValid()) {
            return new StackMobRequestSendResult(StackMobRequestSendResult.RequestSendStatus.FAILED, new StackMobException("Refresh token invalid"));
        }
        return StackMobAccessTokenRequest.newRefreshTokenRequest(executor, session, this.redirectedCallback, callback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * call the logout method on StackMob, invalidating your credentials.
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult logout(StackMobRawCallback callback) {
        session.setOAuth2TokensAndExpiration(null, null, null, 0);
        return new StackMobUserBasedRequest(this.executor,
                                     this.session,
                                     "logout",
                                     StackMobRequest.EmptyParams,
                                     callback,
                                     this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * call the startsession method on StackMob
     * @deprecated
     * @param callback callback to call when the method completes
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult startSession(StackMobRawCallback callback) {
        return new StackMobRequestWithoutPayload(this.executor,
                                          this.session,
                                          HttpVerbWithoutPayload.GET,
                                          StackMobRequest.EmptyHeaders,
                                          StackMobRequest.EmptyParams,
                                          "startsession",
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
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult twitterLogin(String token,
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
        return req.setUrlFormat(this.apiUrlFormat).sendRequest();

    }

    /**
     * update the logged in users’s Twitter status. The logged in user must have a linked Twitter account.
     * @param message the message to send. must be <= 140 characters
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult twitterStatusUpdate(String message,
                                                         StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("tw_st", message);
        return new StackMobUserBasedRequest(this.executor,
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
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult registerWithTwitterToken(String token,
                                                              String secret,
                                                              String username,
                                                              StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("tw_tk", token);
        params.put("tw_ts", secret);
        params.put("username", username);
        return new StackMobUserBasedRequest(this.executor,
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
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult linkUserWithTwitterToken(String token,
                                         String secret,
                                         StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("tw_tk", token);
        params.put("tw_ts", secret);

        return new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            "linkUserWithTwitter",
                                            params,
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     /**
     * login to StackMob with Facebook credentials. The credentials should match a existing user object that has a linked Facebook
     * account, via either {@link #registerWithFacebookToken(String, String, com.stackmob.sdk.callback.StackMobRawCallback)} or
     * {@link #linkUserWithFacebookToken(String, com.stackmob.sdk.callback.StackMobRawCallback)}
     * @param token the facebook user token
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult facebookLogin(String token,
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
        return req.setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * create a new user on StackMob and associate it with an existing Facebook user via Facebook credentials.
     * @param token the facebook user token
     * @param username the StackMob username that the new user should have
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult registerWithFacebookToken(String token,
                                                               String username,
                                                               StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("fb_at", token);
        params.put("username", username);

        return new StackMobUserBasedRequest(this.executor,
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
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult linkUserWithFacebookToken(String token,
                                                               StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("fb_at", token);

        return new StackMobUserBasedRequest(this.executor,
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
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult facebookPostMessage(String msg,
                                                         StackMobRawCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("message", msg);

        return new StackMobUserBasedRequest(this.executor,
                                            this.session,
                                            "postFacebookMessage",
                                            params,
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * get facebook user info for the current user. this method will return nothing if there is no currently logged in FB user
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult getFacebookUserInfo(StackMobRawCallback callback) {
        return new StackMobUserBasedRequest(this.executor, this.session, "getFacebookUserInfo", new HashMap<String, String>(), callback, this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * get twitter user info for the current user. this method will return nothing if there is no currently logged in twitter user
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult getTwitterUserInfo(StackMobRawCallback callback) {
        return new StackMobUserBasedRequest(this.executor, this.session, "getTwitterUserInfo", new HashMap<String, String>(), callback, this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    ////////////////////
    //Push Notifications
    ////////////////////

    /**
     * send a push notification to a group of tokens
     * @param payload the payload of the push notification to send
     * @param tokens the tokens to which to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult pushToTokens(Map<String, String> payload,
                                                  List<StackMobPushToken> tokens,
                                                  StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        Map<String, Object> payloadMap = new HashMap<String, Object>();

        for(StackMobPushToken token : tokens) {
            //Android means GCM in the new sdk for simplicity. Since it meant C2DM in the old sdk we convert tokens back
            if(token.getTokenType() == StackMobPushToken.TokenType.Android && defaultPushType == PushType.C2DM) {
                token.setTokenType(StackMobPushToken.TokenType.AndroidC2DM);
            }
        }
        payloadMap.put("kvPairs", payload);
        finalPayload.put("payload", payloadMap);
        finalPayload.put("tokens", tokens);

        return postPush("push_tokens_universal", finalPayload, callback);
    }

    /**
     * send a push notification to a group of users.
     * @param payload the payload to send
     * @param userIds the IDs of the users to which to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult pushToUsers(Map<String, String> payload,
                                                 List<String> userIds,
                                                 StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        finalPayload.put("kvPairs", payload);
        finalPayload.put("userIds", userIds);
        return postPush("push_users_universal", finalPayload, callback);
    }

    /**
     * register a user for Android Push notifications. This uses GCM unless specified otherwise.
     * @param username the StackMob username to associate with this token
     * @param registrationID the GCM registration ID obtained from GCM see http://developer.android.com/guide/google/gcm/gcm.html#registering for detail on how to get this ID
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult registerForPushWithUser(String username,
                                                             String registrationID,
                                                             StackMobRawCallback callback) {
        return registerForPushWithUser(username, registrationID, false, callback);
    }

    /**
     * register a user for Android Push notifications. This uses GCM unless specified otherwise.
     * @param username the StackMob username to associate with this token
     * @param registrationID the GCM registration ID obtained from GCM see http://developer.android.com/guide/google/gcm/gcm.html#registering for detail on how to get this ID
     * @param overwrite whether to overwrite existing entries
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult registerForPushWithUser(String username,
                                                             String registrationID,
                                                             boolean overwrite,
                                                             StackMobRawCallback callback) {
        RegistrationIDAndUser tokenAndUser = new RegistrationIDAndUser(registrationID, username, defaultPushType, overwrite);
        return postPush("register_device_token_universal", tokenAndUser, callback);
    }

    /**
     * register a user for Android Push notifications.
     * @param username the StackMob username to associate with this token
     * @param token a token containing a registration id and platform
     * @param overwrite whether to overwrite existing entries
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult registerForPushWithUser(String username,
                                                             StackMobPushToken token,
                                                             boolean overwrite,
                                                             StackMobRawCallback callback) {
        RegistrationIDAndUser tokenAndUser = new RegistrationIDAndUser(token.getToken(), username, token.getTokenType(), overwrite);
        return postPush("register_device_token_universal", tokenAndUser, callback);
    }


    /**
     * get all the tokens for the each of the given users
     * @param usernames the users whose tokens to get
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult getTokensForUsers(List<String> usernames,
                                                       StackMobRawCallback callback) {
        final StringBuilder userIds = new StringBuilder();
        boolean first = true;
        for(String username : usernames) {
            if(!first) {
                userIds.append(",");
            }
            first = false;
            userIds.append(username);
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put("userIds", userIds.toString());
        return getPush("get_tokens_for_users_universal", params, callback);
    }

    /**
     * get the user for the each of the given tokens
     * @param tokens the tokens whose users to get
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult getUsersForTokens(List<String> tokens,
                                                       StackMobRawCallback callback) {
        final StringBuilder tokenString = new StringBuilder();
        boolean first = true;
        for(String token : tokens) {
            if(!first) {
                tokenString.append(",");
            }
            first = false;
            tokenString.append(token);
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put("tokens", tokenString.toString());
        return getPush("get_users_for_tokens_universal", params, callback);
    }

    /**
     * broadcast a push notification to all users of this app. use this method sparingly, especially if you have a large app
     * @param payload the payload to broadcast
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult broadcastPushNotification(Map<String, String> payload,
                                                               StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        finalPayload.put("kvPairs", payload);
        return postPush("push_broadcast", finalPayload, callback);
    }

    /**
     * get all expired push tokens for this app.
     * @param clear whether or not to clear the tokens after they've been returned
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    private StackMobRequestSendResult getExpiredPushTokens(Boolean clear,
                                      StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        finalPayload.put("clear", clear);
        return postPush("get_expired_tokens_universal", finalPayload, callback);
    }

    /**
     * get all expired push tokens for this app, and clear them after they've been returned
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult getAndClearExpiredPushTokens(StackMobRawCallback callback) {
        return getExpiredPushTokens(true, callback);
    }

    /**
     * get expired push tokens, but do not clear them after they've been returned
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult getExpiredPushTokens(StackMobRawCallback callback) {
        return getExpiredPushTokens(false, callback);
    }

    /**
     * remove a push token for this app
     * @param tokenString the token value
     * @param tokenType the type of the token
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult removePushToken(String tokenString,
                                                     StackMobPushToken.TokenType tokenType,
                                                     StackMobRawCallback callback) {
        Map<String, Object> finalPayload = new HashMap<String, Object>();
        String type = StackMobPushToken.TokenType.iOS.toString();
        if(tokenType == StackMobPushToken.TokenType.Android) {
            type = (defaultPushType == PushType.C2DM) ? StackMobPushToken.TokenType.AndroidC2DM.toString() : StackMobPushToken.TokenType.Android.toString();
        }
        finalPayload.put("token", tokenString);
        finalPayload.put("type", type);
        return postPush("remove_token_universal", finalPayload, callback);
    }

    ////////////////////
    //GET/PUSH/POST/DELETE/COUNT
    ////////////////////

    /**
     * do a get request on the StackMob platform
     * @param path the path to get
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult get(String path,
                                         StackMobRawCallback callback) {
        return new StackMobRequestWithoutPayload(this.executor,
                                          this.session,
                                          HttpVerbWithoutPayload.GET,
                                          StackMobRequest.EmptyHeaders,
                                          StackMobRequest.EmptyParams,
                                          path,
                                          callback,
                                          this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * do a get request on the StackMob platform
     * @param path the path to get
     * @param arguments arguments to be encoded into the query string of the get request
     * @param headerMap any additional headers to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult get(String path,
                                         Map<String, String> arguments,
                                         Map<String, String> headerMap,
                                         StackMobRawCallback callback) {
        List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
        for(Map.Entry<String, String> header: headerMap.entrySet()) {
            headers.add(header);
        }

        return get(path, arguments, headers, callback);
    }

    /**
     * do a get request on the StackMob platform
     * @param path the path to get
     * @param arguments arguments to be encoded into the query string of the get request
     * @param headers any additional headers to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult get(String path,
                                         Map<String, String> arguments,
                                         List<Map.Entry<String, String>> headers,
                                         StackMobRawCallback callback) {
        return new StackMobRequestWithoutPayload(this.executor,
                                                 this.session,
                                                 HttpVerbWithoutPayload.GET,
                                                 headers,
                                                 arguments,
                                                 path,
                                                 callback,
                                                 this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * do a get request on the StackMob platform
     * @param path the path to get
     * @param arguments the arguments to be encoded into the query string of the get request
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult get(String path,
                                         Map<String, String> arguments,
                                         StackMobRawCallback callback) {
        return this.get(path, arguments, StackMobRequest.EmptyHeaders, callback);
    }

    /**
     * do a get request on the StackMob platform
     * @param query the query to run
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult get(StackMobQuery query,
                                         StackMobRawCallback callback) {
        return this.get("/"+query.getObjectName(), query.getArguments(), query.getHeaders(), callback);
    }

    /**
     * do a get request on the StackMob platform
     * @param query the query to run
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult get(StackMobQueryWithField query,
                    StackMobRawCallback callback) {
        return this.get(query.getQuery(), callback);
    }

    /**
     * do a GET request to the stackmob push service
     * @param path the path of the push request
     * @param arguments the arguments to pass to the push service, in the query string
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request.
     * contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult getPush(String path,
                                             Map<String, String> arguments,
                                             StackMobRawCallback callback) {
        return new StackMobRequestWithoutPayload(this.executor,
                                          this.session,
                                          HttpVerbWithoutPayload.GET,
                                          StackMobRequest.EmptyHeaders,
                                          arguments,
                                          path,
                                          callback,
                                          this.redirectedCallback).setUrlFormat(this.pushUrlFormat).sendRequest();
    }

    /**
     * do a post request on the StackMob platform for a single object
     * @param path the path to get
     * @param requestObject the object to serialize and send in the POST body. this object will be serialized with Gson
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request.
     * contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult post(String path,
                                          Object requestObject,
                                          StackMobRawCallback callback) {
        return new StackMobRequestWithPayload(this.executor,
                                              this.session,
                                              HttpVerbWithPayload.POST,
                                              StackMobRequest.EmptyHeaders,
                                              StackMobRequest.EmptyParams,
                                              requestObject,
                                              path,
                                              callback,
                                              this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * do a post request on the StackMob platform for a single object
     * @param path the path to get
     * @param body the json body
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult post(String path,
                                          String body,
                                          StackMobRawCallback callback) {
        return new StackMobRequestWithPayload(this.executor,
                this.session,
                HttpVerbWithPayload.POST,
                StackMobRequest.EmptyHeaders,
                StackMobRequest.EmptyParams,
                body,
                path,
                callback,
                this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * do a POST request on the StackMob platform for a single object
     * @param path the path to get
     * @param body the json body
     * @param headers any additional headers to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult post(String path,
                                          String body,
                                          List<Map.Entry<String, String>>  headers,
                                          StackMobRawCallback callback) {
        return new StackMobRequestWithPayload(this.executor,
                this.session,
                HttpVerbWithPayload.POST,
                headers,
                StackMobRequest.EmptyParams,
                body,
                path,
                callback,
                this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * do a post request on the StackMob platform with a list of objects
     * @param path the path to get
     * @param requestObjects List of objects to serialize and send in the POST body. the list will be serialized with Gson
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request.
     * contains no information about the response - that will be passed to the callback when the response comes back
     */
    public <T> StackMobRequestSendResult postBulk(String path,
                                                  List<T> requestObjects,
                                                  StackMobRawCallback callback) {
        return new StackMobRequestWithPayload(this.executor,
                                              this.session,
                                              HttpVerbWithPayload.POST,
                                              StackMobRequest.EmptyHeaders,
                                              StackMobRequest.EmptyParams,
                                              requestObjects,
                                              path,
                                              callback,
                                              this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

  /**
   * post a new related object to an existing object. the relation of the root object is updated
   * @param path the path to get
   * @param primaryId id of the object with the relation
   * @param relatedField name of the relation
   * @param relatedObject related object to post
   * @param callback callback to be called when the server returns. may execute in a separate thread
   * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
   */
    public StackMobRequestSendResult postRelated(String path,
                                                 String primaryId,
                                                 String relatedField,
                                                 Object relatedObject,
                            StackMobRawCallback callback) {
      return new StackMobRequestWithPayload(this.executor,
                                            this.session,
                                            HttpVerbWithPayload.POST,
                                            StackMobRequest.EmptyHeaders,
                                            StackMobRequest.EmptyParams,
                                            relatedObject,
                                            String.format("%s/%s/%s", path, primaryId, relatedField),
                                            callback,
                                            this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

  /**
   * post a list of new related objects to an existing object. the relation of the root object is updated
   * @param path the path to get
   * @param primaryId id of the object with the relation
   * @param relatedField name of the relation
   * @param relatedObjects list of related objects to post. the list will be serialized with Gson
   * @param callback callback to be called when the server returns. may execute in a separate thread
   * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
   */
    public <T> StackMobRequestSendResult postRelatedBulk(String path,
                                                         String primaryId,
                                                         String relatedField,
                                                         List<T> relatedObjects,
                                                         StackMobRawCallback callback) {
        return postRelated(path, primaryId, relatedField, relatedObjects, callback);
    }

    private StackMobRequestSendResult postPush(String path,
                          Object requestObject,
                          StackMobRawCallback callback) {
        return new StackMobPushRequest(this.executor,
                                       this.session,
                                       requestObject,
                                       path,
                                       callback,
                                       this.redirectedCallback).setUrlFormat(this.pushUrlFormat).sendRequest();
    }

    /**
     * do a PUT request on the StackMob platform
     * @param path the path to PUT
     * @param id the id of the object to PUT
     * @param requestObject the object to serialize and send in the PUT body. this object will be serialized with Gson
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult put(String path,
                                         String id,
                                         Object requestObject,
                                         StackMobRawCallback callback) {
        return new StackMobRequestWithPayload(this.executor,
                                              this.session,
                                              HttpVerbWithPayload.PUT,
                                              StackMobRequest.EmptyHeaders,
                                              StackMobRequest.EmptyParams,
                                              requestObject,
                                              path + "/" + id,
                                              callback,
                                              this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * do a put request on the StackMob platform
     * @param path the path to put
     * @param id the id of the object to put
     * @param body the json body
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult put(String path,
                                         String id,
                                         String body,
                                         StackMobRawCallback callback) {
        return new StackMobRequestWithPayload(this.executor,
                this.session,
                HttpVerbWithPayload.PUT,
                StackMobRequest.EmptyHeaders,
                StackMobRequest.EmptyParams,
                body,
                path + "/" + id,
                callback,
                this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * do a PUT request on the StackMob platform, treating some of the fields as counters to be incremented rather
     * than as values to set
     * @param path the path to put
     * @param id the id of the object to put
     * @param requestObject the object to serialize and send in the PUT body. this object will be serialized with Gson
     * @param counterFields a list of the fields in the object to be treated as counters being incremented
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult putAndUpdateAtomicCounters(String path,
                                                                String id,
                                                                Object requestObject,
                                                                List<String> counterFields,
                                                                StackMobRawCallback callback) {
        JsonObject obj = new Gson().toJsonTree(requestObject).getAsJsonObject();
        for(Map.Entry<String, JsonElement> field : new HashSet<Map.Entry<String, JsonElement>>(obj.entrySet())) {
            if(counterFields.contains(field.getKey())) {
                obj.remove(field.getKey());
                obj.add(field.getKey() + "[inc]", field.getValue());
            }
        }
        return put(path, id, obj.toString(), callback);
    }

    /**
     * do an atomic update on a an integer field in a particular object and schema
     * @param path the path to put
     * @param id the id of the object to put
     * @param field the field to increment
     * @param value the value to increment by
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult updateAtomicCounter(String path,
                                                         String id,
                                                         String field,
                                                         int value,
                                                         StackMobRawCallback callback) {
        JsonObject body = new JsonObject();
        body.add(field + "[inc]", new JsonPrimitive(value));
        return put(path, id, body.toString(), callback);
    }

    /**
     * do a an atomic put request on the StackMob platform with the contents of the has-many relation
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param relatedField name of the relation
     * @param relatedIds list of ids to atomically add to the relation. The type should be the same type as the primary
     *                   key field of the related object
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public <T> StackMobRequestSendResult putRelated(String path,
                                                    String primaryId,
                                                    String relatedField,
                                                    List<T> relatedIds,
                                                    StackMobRawCallback callback) {
        return new StackMobRequestWithPayload(this.executor,
                                              this.session,
                                              HttpVerbWithPayload.PUT,
                                              StackMobRequest.EmptyHeaders,
                                              StackMobRequest.EmptyParams,
                                              relatedIds,
                                              String.format("%s/%s/%s", path, primaryId, relatedField),
                                              callback,
                                              this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }


    /**
     * do a DELETE request to the StackMob platform
     * @param path the path to delete
     * @param id the id of the object to put
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request.
     * contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult delete(String path,
                                            String id,
                                            StackMobRawCallback callback) {
        return new StackMobRequestWithoutPayload(this.executor,
                                                 this.session,
                                                 HttpVerbWithoutPayload.DELETE,
                                                 StackMobRequest.EmptyHeaders,
                                                 StackMobRequest.EmptyParams,
                                                 path + "/" + id,
                                                 callback,
                                                 this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * atomically remove elements from an array or has many relationship
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param field name of the relation or array field to delete from
     * @param idsToDelete list of ids to atomically remove from field.
     *                    ids should be same type as the primary id of the related type (most likely String or Integer)
     * @param cascadeDeletes true if related objects specified in idsToDelete should also be deleted
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public <T> StackMobRequestSendResult deleteIdsFrom(String path,
                                  String primaryId,
                                  String field,
                                  List<T> idsToDelete,
                                  boolean cascadeDeletes,
                                  StackMobRawCallback callback) {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < idsToDelete.size(); i++) {
            ids.append(idsToDelete.get(i).toString());
            if (i < idsToDelete.size() - 1) {
                ids.append(",");  
            }
        }
        List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
        if (cascadeDeletes) {
            headers.add(new Pair<String, String>("X-StackMob-CascadeDelete", "true"));
        }
        return new StackMobRequestWithoutPayload(this.executor,
                                                 this.session,
                                                 HttpVerbWithoutPayload.DELETE,
                                                 headers,
                                                 StackMobRequest.EmptyParams,
                                                 String.format("%s/%s/%s/%s", path, primaryId, field, ids.toString()),
                                                 callback,
                                                 this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }
  
    /**
     * atomically remove elements from an array or has many relationship
     * @param path the path to get
     * @param primaryId id of the object with the relation
     * @param field name of the relation or array field to delete from
     * @param idToDelete id to atomically remove from field.
     *                   should be same type as the primary id of the related type (most likely String or Integer)
     * @param cascadeDelete true if related object specified in idToDelete should also be deleted
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public <T> StackMobRequestSendResult deleteIdFrom(String path,
                                 String primaryId,
                                 String field,
                                 T idToDelete,
                                 boolean cascadeDelete,
                                 StackMobRawCallback callback) {
        List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
        if (cascadeDelete) {
            headers.add(new Pair<String, String>("X-StackMob-CascadeDelete", "true"));
        }
        return new StackMobRequestWithoutPayload(this.executor,
                                                 this.session,
                                                 HttpVerbWithoutPayload.DELETE,
                                                 headers,
                                                 StackMobRequest.EmptyParams,
                                                 String.format("%s/%s/%s/%s", path, primaryId, field, idToDelete),
                                                 callback,
                                                 this.redirectedCallback).setUrlFormat(this.apiUrlFormat).sendRequest();
    }

    /**
     * retrieve the number of objects for a schema on the StackMob platform
     * @param path the path to get
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult count(String path,
                                         StackMobRawCallback callback) {
        return count(new StackMobQuery(path), callback);
    }

    /**
     * retrieve the number of objects for a query on the StackMob platform
     * @param query the query to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult count(StackMobQuery query,
                                         StackMobRawCallback callback) {
        final StackMobRawCallback userCallback = callback;
        return get(query.isInRange(0, 0), new StackMobRawCallback() {
            @Override
            public void done(HttpVerb requestVerb, String requestURL, List<Map.Entry<String, String>> requestHeaders, String requestBody, Integer responseStatusCode, List<Map.Entry<String, String>> responseHeaders, byte[] responseBody) {
                if(Http.isSuccess(responseStatusCode)) {
                    long count = getTotalNumberOfItemsFromContentRange(responseHeaders);
                    if (count < 0) {
                        try { // No header means all available items were returned, so count them (0 or 1)
                            count = new JsonParser().parse(new String(responseBody)).getAsJsonArray().size();
                        } catch(Exception ignore) {}
                    }
                    responseBody = String.valueOf(count).getBytes();
                }
                userCallback.setDone(requestVerb, requestURL, requestHeaders, requestBody, responseStatusCode, responseHeaders, responseBody);
            }
        });
    }
    
    //Forgot/reset password

    /**
     * send out a password reset email to a user who's forgotten their password
     * @param username the user who's forgotten their password
     * @param callback callback to be called when the server returns. may execute in a separate thread
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */

    public StackMobRequestSendResult forgotPassword(String username,
                                                   StackMobRawCallback callback) {

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        return new StackMobUserBasedRequest(this.executor,
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
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */

    public StackMobRequestSendResult resetPassword(String oldPassword,
                                                   String newPassword,
                                                   StackMobRawCallback callback) {

        Map<String, Map<String, String>> params = new HashMap<String, Map<String, String>>();
        Map<String, String> oldPW = new HashMap<String, String>();
        oldPW.put("password", oldPassword);
        Map<String, String> newPW = new HashMap<String, String>();
        newPW.put("password", newPassword);
        params.put("old", oldPW);
        params.put("new", newPW);
        return new StackMobUserBasedRequest(this.executor,
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
     * @return a StackMobRequestSendResult representing what happened when the SDK tried to do the request. contains no information about the response - that will be passed to the callback when the response comes back
     */
    public StackMobRequestSendResult getLoggedInUser(StackMobCallback callback) {
        return get("user/loggedInUser", callback);
    }

    /**
     * get the logged in user locally. This method is deprecated and {@link #getLoggedInUser(com.stackmob.sdk.callback.StackMobCallback)} should be
     * use instead
     * @deprecated
     * @return the logged in user
     */
    public String getLoggedInUser() {
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
     * @deprecated
     * @param username the user to check
     * @return whether that user is logged in
     */
    public boolean isUserLoggedIn(String username) {
        return username != null && username.equals(this.getLoggedInUser());
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
