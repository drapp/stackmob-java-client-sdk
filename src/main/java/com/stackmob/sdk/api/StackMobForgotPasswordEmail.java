/**
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
package com.stackmob.sdk.api;

import com.google.gson.*;
import com.stackmob.sdk.push.StackMobPushToken;

import java.lang.reflect.Type;

/**
 * This class is essentially a string that, when used in a StackMobModel class, will be inferred as an email suitable for use in a forgot password email flow
 */
public class StackMobForgotPasswordEmail {
    private String email;

    public StackMobForgotPasswordEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public static class Serializer implements JsonSerializer<StackMobForgotPasswordEmail>{
        public JsonElement serialize(StackMobForgotPasswordEmail fpEmail, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(fpEmail.getEmail());
        }
    }

    public static class Deserializer implements JsonDeserializer<StackMobForgotPasswordEmail> {
        public StackMobForgotPasswordEmail deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            return new StackMobForgotPasswordEmail(primitive.getAsString());
        }
    }
}
