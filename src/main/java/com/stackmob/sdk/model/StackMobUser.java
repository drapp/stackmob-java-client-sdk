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
public class StackMobUser extends StackMobModel {

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
     * create a new StackMobUser directly with a username and password.
     * @param username the user's username
     * @param password the user's password
     */
    public StackMobUser(String username, String password) {
        this(StackMobUser.class, username, password);
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
                try {
                    fillFromJson(responseBody);
                } catch (StackMobException e) {
                    StackMob.getLogger().logWarning("Error filling in user model from login" + e);
                }
                super.success(responseBody);
            }
        });
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
        return StackMob.getStackMob().isLoggedIn();
    }

    /**
     * reset this user's passwords. Passwords cannot be changed directly when saving objects.
     * @param oldPassword the users' old password
     * @param newPassword the new password
     * @param callback invoked upon completed reset
     */
    public void resetPassword(String oldPassword, String newPassword, StackMobCallback callback) {
        StackMob.getStackMob().resetPassword(oldPassword, newPassword, callback);
    }

    /**
     * register this user for push
     * @param registrationID the Android registration id to associate with this user
     * @param callback invoked when the operation is complete
     */
    public void registerForPush(String registrationID, StackMobRawCallback callback) {
        StackMob.getStackMob().registerForPushWithUser(getID(), registrationID, callback);
    }

    /**
     * retrieve the push token associated with this user
     * @param callback invoked when the operation is complete
     */
    public void getPushToken(StackMobRawCallback callback) {
        StackMob.getStackMob().getTokensForUsers(Arrays.asList(getID()), callback);
    }

}
