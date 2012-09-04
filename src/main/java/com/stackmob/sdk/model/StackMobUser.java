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

import com.google.gson.JsonParser;
import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.callback.StackMobIntermediaryCallback;
import com.stackmob.sdk.callback.StackMobQueryCallback;
import com.stackmob.sdk.callback.StackMobRawCallback;
import com.stackmob.sdk.exception.StackMobException;
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


    /**
     * send out a password reset email to a user who's forgotten their password
     * @param username the user who's forgotten their password
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public static void sentForgotPasswordEmail(String username, StackMobCallback callback) {
        StackMob.getStackMob().forgotPassword(username, callback);
    }

    /**
     * send a push notification to a group of users.
     * @param payload the payload to send
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public <T extends StackMobUser> void pushToMultiple(Map<String, String> payload, List<T> users, StackMobRawCallback callback) {
        List<String> userIds = new ArrayList<String>();
        for(T user : users) {
            userIds.add(user.getID());
        }
        StackMob.getStackMob().getPush().pushToUsers(payload, userIds, callback);
    }

    /**
     * Get the currently logged in user. Use to get the user object in place of login when you're starting your app and you find that you're still logged in (via {@link com.stackmob.sdk.api.StackMob#isLoggedIn()}).
     * @param classOfT The class of the user model
     * @param callback The callback to invoke with the user model
     */
    public static <T extends StackMobUser> void getLoggedInUser(final Class<T> classOfT, final StackMobQueryCallback<T> callback) {
        StackMob.getStackMob().getLoggedInUser(new StackMobCallback(){
            @Override
            public void success(String responseBody) {
                List<T> list = new ArrayList<T>();
                try {
                    list.add(StackMobModel.newFromJson(classOfT, responseBody));
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
    
    String password;

    /**
     * create a new user of the specified class with a username and password.
     * @param actualClass the subclass being constructed
     * @param username the user's username
     * @param password the user's password
     */
    protected StackMobUser(Class<? extends StackMobUser> actualClass, String username, String password) {
        super(actualClass);
        setID(username);
        this.password = password;
    }

    /**
     * create a new user of the specified class with a username.
     * @param actualClass the subclass being constructed
     * @param username the user's username
     */
    protected StackMobUser(Class<? extends StackMobUser> actualClass, String username) {
        this(actualClass, username, null);
    }

    /**
     * create a new user of the specified class
     * @param actualClass the subclass being constructed
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
     * get the username, which also serves as a primary key
     * @return the username
     */
    public String getUsername() {
        return getID();
    }

    private Map<String, String> getLoginArgs() {
        Map<String, String> args = new HashMap<String, String>();
        args.put("username", getID());
        args.put("password", password);
        return args;
    }

    /**
     * log this user into StackMob with specialized info. This will clear the password from the class.
     * @param args key value pair arguments
     * @param callback invoked on completed login attempt
     */
    protected void login(Map<String, String> args, StackMobCallback callback) {
        StackMob.getStackMob().login(args, new StackMobIntermediaryCallback(callback){
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
     * log this user into StackMob. This will clear the password from the class.
     * @param callback invoked on completed login attempt
     */
    public void login(StackMobCallback callback) {
        login(getLoginArgs(), callback);
    }

    /**
     * log this user into StackMob with their temporary password and reset their password. This is
     * used as part of the <a href="https://stackmob.com/devcenter/docs/User-Authentication-API#a-forgot_password">forgot password flow</a>
     * @param callback invoked on completed login attempt
     */
    public void loginResettingTemporaryPassword(String newPassword, StackMobCallback callback) {
        Map<String, String> args = getLoginArgs();
        args.put("new_password", newPassword);
        login(args, callback);
    }

    /**
     * login to StackMob with Facebook credentials. The credentials should match a existing user object that has a linked Facebook
     * account, via either {@link #createWithFacebook(String, com.stackmob.sdk.callback.StackMobCallback)} or
     * {@link #linkWithFacebook(String, com.stackmob.sdk.callback.StackMobCallback)}
     * @param facebookToken the facebook user token
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void loginWithFacebook(String facebookToken, StackMobCallback callback) {
        StackMob.getStackMob().facebookLogin(facebookToken, new StackMobIntermediaryCallback(callback){
            @Override
            public void success(String responseBody) {
                fillUserFromJson(responseBody);
                super.success(responseBody);
            }

        });
    }

    /**
     * login to StackMob with twitter credentials. The credentials should match a existing user object that has a linked Twitter
     * account, via either {@link #createWithTwitter(String, String, com.stackmob.sdk.callback.StackMobCallback)} or
     * {@link #linkWithTwitter(String, String, com.stackmob.sdk.callback.StackMobCallback)}
     * @param twitterToken the twitter session key (this is a per user key - different from the consumer key)
     * @param twitterSecret the twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void loginWithTwitter(String twitterToken, String twitterSecret, StackMobCallback callback) {
        StackMob.getStackMob().twitterLogin(twitterToken, twitterSecret, new StackMobIntermediaryCallback(callback) {
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
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void refreshLogin(StackMobCallback callback) {
        StackMob.getStackMob().refreshLogin(callback);
    }

    /**
     * log the user out, clearing all credential
     * @param callback invoked when logout is complete
     */
    public void logout(StackMobCallback callback) {
        StackMob.getStackMob().logout(callback);
    }

    /**
     * check if the user is logged in
     * @return whether the user is logged in
     */
    public boolean isLoggedIn() {
        return StackMob.getStackMob().isUserLoggedIn(getID());
    }

    /**
     * check whether a {@link #refreshLogin(com.stackmob.sdk.callback.StackMobCallback)} call is required
     * to continue making authenticated requests. This will happen automatically, so there's no reason to
     * check this method unless you're overriding the existing refresh token system. If there are no credentials
     * at all this will be false.
     * @return whether there's a valid refresh token that can be used to refresh the login
     */
    public boolean refreshRequired() {
        return isLoggedIn() && StackMob.getStackMob().refreshRequired();
    }

    /**
     * create this user on StackMob and associate it with an existing Facebook user via Facebook credentials.
     * @param facebookToken the facebook user token
     * @param callback callback to be called when the server returns. may execute in a separate thread
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
     * create thsi user on StackMob and associate it with an existing Twitter user via Twitter credentials.
     * @param twitterToken the twitter session key (this is a per user key - different from the consumer key)
     * @param twitterSecret the twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback callback to be called when the server returns. may execute in a separate thread
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
     * link an user with an existing Facebook user via Facebook credentials. The user must be logged in
     * @param facebookToken the Facebook user token
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void linkWithFacebook(String facebookToken, StackMobCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().linkUserWithFacebookToken(facebookToken, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * link an user with an existing Facebook user via Facebook credentials. The user must be logged in
     * @param twitterToken the twitter session key (this is a per user key - different from the consumer key)
     * @param twitterSecret the twitter session secret (this is a per user secret - different from the consumer secret)
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void linkWithTwitter(String twitterToken, String twitterSecret, StackMobCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().linkUserWithTwitterToken(twitterToken, twitterSecret, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * post a message to Facebook. This method will not post to FB and will return nothing if there is no user logged into FB.
     * @param msg the message to post
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void postFacebookMessage(String msg, StackMobRawCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().facebookPostMessage(msg, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * update the logged in users’s Twitter status. The logged in user must have a linked Twitter account.
     * @param message the message to send. must be <= 140 characters
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void postTwitterUpdate(String message, StackMobRawCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().twitterStatusUpdate(message, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * get facebook user info for the current user. this method will return nothing if there is no currently logged in FB user
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void getFacebookUserInfo(StackMobRawCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().getFacebookUserInfo(callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * get twitter user info for the current user. this method will return nothing if there is no currently logged in twitter user
     * @param callback callback to be called when the server returns. may execute in a separate thread
     */
    public void getTwitterUserInfo(StackMobRawCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().getTwitterUserInfo(callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * reset this user's passwords. Passwords cannot be changed directly when saving objects.
     * @param oldPassword the users' old password
     * @param newPassword the new password
     * @param callback invoked upon completed reset
     */
    public void resetPassword(String oldPassword, String newPassword, StackMobCallback callback) {
        if(isLoggedIn()) {
            StackMob.getStackMob().resetPassword(oldPassword, newPassword, callback);
        } else {
            callback.unsent(new StackMobException("User not logged in"));
        }
    }

    /**
     * register this user for push
     * @param registrationID the Android registration id to associate with this user
     * @param callback invoked when the operation is complete
     */
    public void registerForPush(String registrationID, StackMobRawCallback callback) {
        StackMob.getStackMob().getPush().registerForPushWithUser(getID(), registrationID, callback);
    }

    /**
     * retrieve the push token associated with this user
     * @param callback invoked when the operation is complete
     */
    public void getPushToken(StackMobRawCallback callback) {
        StackMob.getStackMob().getPush().getTokensForUsers(Arrays.asList(getID()), callback);
    }

}
