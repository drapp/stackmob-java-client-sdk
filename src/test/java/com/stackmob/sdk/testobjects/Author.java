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
package com.stackmob.sdk.testobjects;

import com.stackmob.sdk.api.StackMobGeoPoint;
import com.stackmob.sdk.model.StackMobModel;

public class Author extends StackMobModel {

    private String name;
    private StackMobGeoPoint birthPlace;


    public Author(String name) {
        this(name, null);
    }

    public Author(String name, StackMobGeoPoint birthPlace) {
        super(Author.class);
        setName(name);
        this.birthPlace = birthPlace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
