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
package com.stackmob.sdk.model;

import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.api.StackMobOptions;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.callback.StackMobIntermediaryCallback;
import com.stackmob.sdk.callback.StackMobQueryCallback;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.push.StackMobPush;
import com.stackmob.sdk.push.StackMobPushToken;

import java.util.*;

/**
 * A specialized subclass of StackMobModel to represent users of your app. Extend this class with the fields you want and you have an object that knows how to do logins as
 * well as synchronize itself with the cloud.
 * <pre>
 * {@code
 * public class User extends StackMobUser {
 *
 *     private String email;
 *     private List<User> friends;
 *     private List<TaskList> taskLists;
 *
 *     public User(String username, String password, String email) {
 *         super(User.class, username, password);
 *         this.email = email;
 *     }
 *
 *     //Add whatever setters/getters/other functionality you want here
 * }
 * }
 * </pre>
 * There is a built in concept of username and password, so you shouldn't declare fields for those. You must follow the same rules for fields as you would for {@link StackMobModel}.
 * Most likely your app will begin like this
 * <pre>
 * {@code
 * if(stackmob.isLoggedIn()) {
 *     User.getLoggedInUser(User.class, new StackMobQueryCallback<User>() {
 *         public void success(List<User> result) {
 *             startAppWithUserData(result.get(0));
 *         }
 *
 *         public void failure(StackMobException e) {
 *             // handle failure
 *         }
 *     });
 * } else {
 *     // show login screen to get credentials
 *     final User theUser = new User(username, password);
 *     theUser.login(new StackMobModelCallback() {
 *         public void success() {
 *             startAppWithUserData(theUser);
 *         }
 *
 *         public void failure(StackMobException e) {
 *             // handle failure case
 *         }
 *     });
 * }
 * }
 * </pre>
 * After either path you've got a confirmed logged in user and their user data.
 *
 *
 */
public abstract class StackMobUser extends StackMobModel {


    @Override
    public void save(StackMobOptions options, StackMobCallback callback) {
        super.save(password == null ? options : options.suggestHTTPS(true), callback);
    }


    /**
     * Send out a password reset email to a user who's forgotten their password.
     * @param username The user who's forgotten their password
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public static void sentForgotPasswordEmail(String username, StackMobCallback callback) {
        StackMob.getStackMob().forgotPassword(username, callback);
    }

    /**
     * Send a push notification to a group of users.
     * @param payload The payload to send
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public static <T extends StackMobUser> void pushToMultiple(Map<String, String> payload, List<T> users, StackMobRawCallback callback) {
        List<String> userIds = new ArrayList<String>();
        for(T user : users) {
            userIds.add(user.getID());
        }
        StackMobPush.getPush().pushToUsers(payload, userIds, callback);
    }

    /**
     * Get the currently logged in user. Use to get the user object in place of login when you're starting your app and you find that you're still logged in (via {@link com.stackmob.sdk.api.StackMob#isLoggedIn()}).
     * @param classOfT The class of the user model
     * @param callback The callback to invoke with the user model
     */
    public static <T extends StackMobUser> void getLoggedInUser(final Class<T> classOfT, final StackMobQueryCallback<T> callback) {
        getLoggedInUser(classOfT, new StackMobOptions(), callback);
    }

    /**
     * Get the currently logged in user. Use to get the user object in place of login when you're starting your app and you find that you're still logged in (via {@link com.stackmob.sdk.api.StackMob#isLoggedIn()}).
     * @param classOfT The class of the user model
     * @param options Additional options, such as headers, to modify the request
     * @param callback The callback to invoke with the user model
     */
    public static <T extends StackMobUser> void getLoggedInUser(final Class<T> classOfT, StackMobOptions options, final StackMobQueryCallback<T> callback) {
       getLoggedInUser(StackMob.getStackMob(), classOfT, options, callback);
    }

    /**
     * Get the currently logged in user. Use to get the user object in place of login when you're starting your app and you find that you're still logged in (via {@link com.stackmob.sdk.api.StackMob#isLoggedIn()}).
     * @param classOfT The class of the user model
     * @param options Additional options, such as headers, to modify the request
     * @param callback The callback to invoke with the user model
     */
    public static <T extends StackMobUser> void getLoggedInUser(final StackMob stackmob, final Class<T> classOfT, StackMobOptions options, final StackMobQueryCallback<T> callback) {
        StackMob.getStackMob().getLoggedInUser(options, new StackMobCallback(){
            @Override
            public void success(String responseBody) {
                List<T> list = new ArrayList<T>();
                try {
                    list.add(StackMobModel.newFromJson(stackmob, classOfT, responseBody));
                    callback.success(list);
                } catch(Exception e) {
                    callback.failure(new StackMobException(e.getMessage()));
                }
            }

            @Override
            public void failure(StackMobException e) {
                callback.failure(e);
            }
        });
    }

    /**
     * Get the username for the logged in user, if one exists. This method is deprecated and
     * {@link StackMob#getLoggedInUser(com.stackmob.sdk.callback.StackMobCallback)} should be used instead.
     * @return The logged in user's username
     */
    @Deprecated
    public static String getLoggedInUsername() {
        return StackMob.getStackMob().getLoggedInUsername();
    }

    String password;

    /**
     * Create a new user of the specified class with a username and password.
     * @param actualClass The subclass being constructed
     * @param username The user's username
     * @param password The user's password
     */
    protected StackMobUser(Class<? extends StackMobUser> actualClass, String username, String password) {
        super(actualClass);
        setID(username);
        this.password = password;
    }

    /**
     * Create a new user of the specified class with a username.
     * @param actualClass The subclass being constructed
     * @param username The user's username
     */
    protected StackMobUser(Class<? extends StackMobUser> actualClass, String username) {
        this(actualClass, username, null);
    }

    /**
     * Create a new user of the specified class.
     * @param actualClass The subclass being constructed
     */
    protected StackMobUser(Class<? extends StackMobUser> actualClass) {
        this(actualClass, null);
    }

    @Override
    public String getSchemaName() {
        return StackMob.getStackMob().getSession().getUserObjectName();
    }

    @Override
    public String getIDFieldName() {
        return "username";
    }

    /**
     * Get the username, which also serves as a primary key.
     * @return The username
     */
    public String getUsername() {
        return getID();
    }

    private Map<String, String> getLoginArgs() {
        Map<String, String> args = new HashMap<String, String>();
        args.put(StackMob.getStackMob().getUserIdName(), getID());
        args.put(StackMob.getStackMob().getPasswordField(), password);
        return args;
    }

    /**
     * Log this user into StackMob with specialized info. This will clear the password from the class.
     * @param args Key/value pair arguments
     * @param options Additional options, such as headers, to modify the request
     * @param callback Invoked on completed login attempt
     */
    protected void login(Map<String, String> args, StackMobOptions options, StackMobCallback callback) {
        StackMob.getStackMob().login(args, options, new StackMobIntermediaryCallback(callback){
            @Override
            public void success(String responseBody) {
                // Don't keep the password around after login
                password = null;
                fillUserFromJson(responseBody);
                super.success(responseBody);
            }
        });
    }

    private void fillUserFromJson(String responseBody) {
        try {
            fillFromJson(responseBody);
        } catch (StackMobException e) {
            StackMob.getStackMob().getSession().getLogger().logWarning("Error filling in user model from login" + e);
        }
    }

    /**
     * Log this user into StackMob. This will clear the password from the class.
     * @param callback Invoked on completed login attempt
     */
    public void login(StackMobCallback callback) {
        login(getLoginArgs(), new StackMobOptions(), callback);
    }

    /**
     * Log this user into StackMob. This will clear the password from the class.
     * @param options Additional options, such as headers, to modify the request
     * @param callback Invoked on completed login attempt
     */
    public void login(StackMobOptions options, StackMobCallback callback) {
        login(getLoginArgs(), options, callback);
    }

    /**
     * Log this user into StackMob with their temporary password and reset their password. This should be used
     * when the {@link com.stackmob.sdk.callback.StackMobRawCallback#temporaryPasswordResetRequired(com.stackmob.sdk.exception.StackMobException)}
     * callback is called. This is used as part of the <a href="https://www.stackmob.com/devcenter/docs/StackMob/StackMob-Forgot-Password-Tutorial">forgot password flow</a>.
     * @param callback Invoked on completed login attempt
     */
    public void loginResettingTemporaryPassword(String newPassword, StackMobCallback callback) {
        loginResettingTemporaryPassword(newPassword, new StackMobOptions(), callback);
    }

    /**
     * Log this user into StackMob with their temporary password and reset their password. This should be used
     * when the {@link com.stackmob.sdk.callback.StackMobRawCallback#temporaryPasswordResetRequired(com.stackmob.sdk.exception.StackMobException)}
     * callback is called. This is used as part of the <a href="https://www.stackmob.com/devcenter/docs/StackMob/StackMob-Forgot-Password-Tutorial">forgot password flow</a>.
     * @param options Additional options, such as headers, to modify the request
     * @param callback Invoked on completed login attempt
     */
    public void loginResettingTemporaryPassword(String newPassword, StackMobOptions options, StackMobCallback callback) {
        Map<String, String> args = getLoginArgs();
        args.put("new_password", newPassword);
        login(args, options, callback);
    }

    /**
     * Login to StackMob with Facebook credentials. The credentials should match an existing user object that has a linked Facebook
     * account, via either {@link #createWithFacebook(String, com.stackmob.sdk.callback.StackMobCallback)} or
     * {@link #linkWithFacebook(String, com.stackmob.sdk.callback.StackMobCallback)}.
     * @param facebookToken The facebook user token
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void loginWithFacebook(String facebookToken, StackMobCallback callback) {
        loginWithFacebook(facebookToken, false, null, new StackMobOptions(), callback);
    }

    /**
     * Login to StackMob with Facebook credentials. The method includes the option to create a StackMob user if one didn't exist before.
     * Otherwise, the credentials should match an existing user object that has a linked Facebook
     * account, via either {@link #createWithFacebook(String, com.stackmob.sdk.callback.StackMobCallback)} or
     * {@link #linkWithFacebook(String, com.stackmob.sdk.callback.StackMobCallback)}.
     * @param facebookToken The facebook user token
     * @param createUser Pass true to create a new user if no existing user is associated with the provided token. This works with OAuth2 only.
     * @param username If createUser is true, the primary key (username) to give the created user.
     * @param options Additional options, such as headers, to modify the request
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void loginWithFacebook(String facebookToken, boolean createUser, String username, StackMobOptions options, StackMobCallback callback) {
        StackMob.getStackMob().facebookLogin(facebookToken, createUser, username, options, new StackMobIntermediaryCallback(callback){
            @Override
            public void success(String responseBody) {
                fillUserFromJson(responseBody);
                super.success(responseBody);
            }

        });
    }

    /**
     * Login to StackMob with twitter credentials. The credentials should match an existing user object that has a linked Twitter
     * account, via either {@link #createWithTwitter(String, String, com.stackmob.sdk.callback.StackMobCallback)} or
     * {@link #linkWithTwitter(String, String, com.stackmob.sdk.callback.StackMobCallback)}.
     * @param twitterToken The twitter session key (this is a per user key - different from the consumer key)
     * @param twitterSecret The twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void loginWithTwitter(String twitterToken, String twitterSecret, StackMobCallback callback) {
        loginWithTwitter(twitterToken, twitterSecret, false, null,  new StackMobOptions(), callback);
    }

    /**
     * Login to StackMob with twitter credentials. The method includes the option to create a StackMob user if one didn't exist before.
     * Otherwise, the credentials should match an existing user object that has a linked Twitter
     * account, via either {@link #createWithTwitter(String, String, com.stackmob.sdk.callback.StackMobCallback)} or
     * {@link #linkWithTwitter(String, String, com.stackmob.sdk.callback.StackMobCallback)}.
     * @param twitterToken The twitter session key (this is a per user key - different from the consumer key)
     * @param twitterSecret The twitter session secret (this is a per user secret - different from the consumer secret)
     * @param createUser Pass true to create a new user if no existing user is associated with the provided tokens. This works with OAuth2 only.
     * @param username If createUser is true, the primary key (username) to give the created user.
     * @param options Additional options, such as headers, to modify the request
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void loginWithTwitter(String twitterToken, String twitterSecret, boolean createUser, String username, StackMobOptions options, StackMobCallback callback) {
        StackMob.getStackMob().twitterLogin(twitterToken, twitterSecret, createUser, username, options, new StackMobIntermediaryCallback(callback) {
            @Override
            public void success(String responseBody) {
                fillUserFromJson(responseBody);
                super.success(responseBody);
            }

        });
    }

    /**
     * Login to StackMob with gigya credentials. If a corresponding StackMob user didn't exist before, it will
     * be created.
     * @param gigyaUid The parameter UID
     * @param timestamp The parameter signatureTimestamp
     * @param sig The parameter UIDSignature
     * @param options Additional options, such as headers, to modify the request
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void loginWithGigya(String gigyaUid, String timestamp, String sig, StackMobOptions options, StackMobCallback callback) {
        StackMob.getStackMob().gigyaLogin(gigyaUid, timestamp, sig, options, new StackMobIntermediaryCallback(callback) {
          @Override
          public void success(String responseBody) {
              fillUserFromJson(responseBody);
              super.success(responseBody);
          }
        });
    }

    /**
     * Refresh the current OAuth2 login. This ordinarily happens automatically, but this method
     * can give you finer control if you need it. Logins last an hour by default. Once they expire
     * they need to be refreshed. Make sure not to send multiple refresh token requests.
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void refreshLogin(StackMobCallback callback) {
        StackMob.getStackMob().refreshLogin(callback);
    }

    /**
     * Log the user out, clearing all credential.
     * @param callback invoked when logout is complete
     */
    public void logout(StackMobCallback callback) {
        StackMob.getStackMob().logout(callback);
    }

    /**
     * Check if the user is logged in.
     * @return whether the user is logged in
     */
    public boolean isLoggedIn() {
        return StackMob.getStackMob().isUserLoggedIn(getID());
    }

    /**
     * Check whether a {@link #refreshLogin(com.stackmob.sdk.callback.StackMobCallback)} call is required
     * to continue making authenticated requests. This will happen automatically, so there's no reason to
     * check this method unless you're overriding the existing refresh token system. If there are no credentials
     * at all this will be false.
     * @return Whether there's a valid refresh token that can be used to refresh the login
     */
    public boolean refreshRequired() {
        return isLoggedIn() && StackMob.getStackMob().refreshRequired();
    }

    /**
     * Create this user on StackMob and associate it with an existing Facebook user via Facebook credentials.
     * @param facebookToken The facebook user token
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void createWithFacebook(String facebookToken, StackMobCallback callback) {
        StackMob.getStackMob().registerWithFacebookToken(facebookToken, getID(), new StackMobIntermediaryCallback(callback) {
            @Override
            public void success(String responseBody) {
                fillUserFromJson(responseBody);
                super.success(responseBody);
            }

        });
    }

    /**
     * Create this user on StackMob and associate it with an existing Twitter user via Twitter credentials.
     * @param twitterToken The twitter session key (this is a per user key - different from the consumer key)
     * @param twitterSecret The twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback Callback to be called when the server returns. May execute in a separate thread
     */
    public void createWithTwitter(String twitterToken, String twitterSecret, StackMobCallback callback) {
        StackMob.getStackMob().registerWithTwitterToken(twitterToken, twitterSecret, getID(), new StackMobIntermediaryCallback(callback) {
            @Override
            public void success(String responseBody) {
                fillUserFromJson(responseBody);
                super.success(responseBody);
            }

        });
    }

    /**
     * Link a user with an existing Facebook user via Facebook credentials. The user must be logged in.
     * @param facebookToken The Facebook user token
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void linkWithFacebook(String facebookToken, StackMobCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().linkUserWithFacebookToken(facebookToken, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Unlink a user from their Facebook credentials, if linked. The user must be logged in.
     * @param callback Callback to be called when the server returns. May execute in a separate thread.
     */
    public void unlinkFromFacebook(StackMobCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().unlinkUserFromFacebook(callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Link a user with an existing Facebook user via Facebook credentials. The user must be logged in.
     * @param twitterToken The twitter session key (this is a per user key - different from the consumer key)
     * @param twitterSecret The twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void linkWithTwitter(String twitterToken, String twitterSecret, StackMobCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().linkUserWithTwitterToken(twitterToken, twitterSecret, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Unlink a user from their Twitter credentials, if linked. The user must be logged in.
     * @param callback Callback to be called when the server returns. May execute in a separate thread.
     */
    public void unlinkFromTwitter(StackMobCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().unlinkUserFromTwitter(callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Post a message to Facebook. This method will not post to FB and will return nothing if there is no user logged into FB.
     * @param msg The message to post
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void postFacebookMessage(String msg, StackMobRawCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().facebookPostMessage(msg, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Update the logged in user’s Twitter status. The logged in user must have a linked Twitter account.
     * @param message The message to send. must be <= 140 characters
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void postTwitterUpdate(String message, StackMobRawCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().twitterStatusUpdate(message, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Get Facebook user info for the current user. This method will return nothing if there is no currently logged in FB user.
     * @param callback Callback to be called when the server returns. may execute in a separate thread
     */
    public void getFacebookUserInfo(StackMobRawCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().getFacebookUserInfo(callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Get Twitter user info for the current user. This method will return nothing if there is no currently logged in Twitter user.
     * @param callback Callback to be called when the server returns. May execute in a separate thread
     */
    public void getTwitterUserInfo(StackMobRawCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().getTwitterUserInfo(callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Reset this user's passwords. Passwords cannot be changed directly when saving objects.
     * @param oldPassword The users' old password
     * @param newPassword The new password
     * @param callback Invoked upon completed reset
     */
    public void resetPassword(String oldPassword, String newPassword, StackMobCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().resetPassword(oldPassword, newPassword, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * Register this user for push.
     * @param token The Android registration id to associate with this user
     * @param callback Invoked when the operation is complete
     */
    public void registerForPush(StackMobPushToken token, StackMobRawCallback callback) {
        StackMobPush.getPush().registerForPushWithUser(token, getID(), callback);
    }

    /**
     * Send a push message to this user.
     * @param payload The message to send
     * @param callback Invoked when the operation is complete
     */
    public void sendPush(Map<String, String> payload, StackMobRawCallback callback) {
        StackMobPush.getPush().pushToUsers(payload, Arrays.asList(getID()), callback);
    }

    /**
     * Remove this token from push.
     * @param token The token to remove
     * @param callback Invoked when the operation is complete
     */
    public void removeFromPush(StackMobPushToken token, StackMobRawCallback callback) {
        StackMobPush.getPush().removePushToken(token, callback);
    }

    /**
     * Retrieve the push tokens associated with this user.
     * @param callback Invoked when the operation is complete
     */
    public void getPushTokens(StackMobRawCallback callback) {
        StackMobPush.getPush().getTokensForUsers(Arrays.asList(getID()), callback);
    }

}
