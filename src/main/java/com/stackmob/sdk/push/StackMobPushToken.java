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

package com.stackmob.sdk.push;

/**
 * A push token identifies a specific device for push on a particular platform. The token can then be registered with StackMob and pushed to.
 *
 * @see com.stackmob.sdk.push.StackMobPush#registerForPushWithUser(StackMobPushToken, String, boolean, com.stackmob.sdk.callback.StackMobRawCallback)
 * @see com.stackmob.sdk.push.StackMobPush#pushToTokens(java.util.Map, java.util.List, com.stackmob.sdk.callback.StackMobRawCallback)
 */
public class StackMobPushToken {

    /**
     * The supported push platforms a token can be from.
     */
    public static enum TokenType {
        iOS("ios"),
        Android("androidGCM"),
        AndroidC2DM("android");

        private String type;
        TokenType(String type) {
            this.type = type;
        }

        public String toString() {
            return type;
        }
    }

    private static StackMobPushToken.TokenType defaultPushType = StackMobPushToken.TokenType.Android;

    /**
     * Sets the type of push this StackMob instance will do. The default is
     * GCM. Use this to switch back to C2DM if you need to
     * @param type C2DM or GCM
     */
    public static void setPushType(StackMobPushToken.TokenType type) {
        defaultPushType = type;
    }

    private String tokenString;
    private TokenType type;

    /**
     * create a token with a string and the default platform
     * @param token the token string
     */
    public StackMobPushToken(String token) {
        this(token, defaultPushType);
    }

    /**
     * create a token with a string and type
     * @param token the token string
     * @param type the platform you're using
     */
    public StackMobPushToken(String token, TokenType type) {
        this.tokenString = token;
        this.type = type;
    }

    /**
     * get the token string
     * @return the token string
     */
    public String getToken() {
        return tokenString;
    }

    /**
     * get the token type
     * @return the type
     */
    public TokenType getTokenType() {
        return type;
    }

    /**
     * change the token type
     * @param type the new type
     */
    public void setTokenType(TokenType type) {
        this.type = type;
    }

}
