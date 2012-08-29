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
 */
public class StackMobUser extends StackMobModel {

    /**
     * Get the currently logged in user
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

    protected StackMobUser(Class<? extends StackMobUser> actualClass, String username, String password) {
        super(actualClass);
        setID(username);
        this.password = password;
    }
    
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

    public String getUsername() {
        return getID();
    }

    private Map<String, String> getLoginArgs() {
        Map<String, String> args = new HashMap<String, String>();
        args.put("username", getID());
        args.put("password", password);
        return args;
    }
    
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

    public void login(StackMobCallback callback) {
        login(getLoginArgs(), callback);
    }
    
    public void loginResettingTemporaryPassword(String newPassword, StackMobCallback callback) {
        Map<String, String> args = getLoginArgs();
        args.put("new_password", newPassword);
        login(args, callback);
    }

    public void logout(StackMobCallback callback) {
        StackMob.getStackMob().logout(callback);
    }

    public boolean isLoggedIn() {
        return StackMob.getStackMob().isUserLoggedIn(getID());
    }

    public void resetPassword(String oldPassword, String newPassword, StackMobCallback callback) {
        StackMob.getStackMob().resetPassword(oldPassword, newPassword, callback);
    }

    public void registerForPush(String registrationID, StackMobRawCallback callback) {
        StackMob.getStackMob().registerForPushWithUser(getID(), registrationID, callback);
    }

    public void getPushToken(StackMobRawCallback callback) {
        StackMob.getStackMob().getTokensForUsers(Arrays.asList(getID()), callback);
    }

}
