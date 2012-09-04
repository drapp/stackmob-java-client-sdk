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

import com.stackmob.sdk.util.ListHelpers;
import com.stackmob.sdk.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StackMobOptions {
    private List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();

    private static final String SelectHeader = "X-StackMob-Select";
    private static final String ExpandHeader = "X-StackMob-Expand";

    public StackMobOptions header(String name, String value) {
        this.headers.add(new Pair<String, String>(name, value));
        return this;
    }

    public StackMobOptions headers(Map<String, String> headerMap) {
        for(Map.Entry<String, String> header: headerMap.entrySet()) {
            this.headers.add(header);
        }
        return this;
    }

    public StackMobOptions headers(List<Map.Entry<String, String>> headers) {
        this.headers.addAll(headers);
        return this;
    }


    /**
     * restricts the fields returned in the query
     * @param fields the fields to return
     * @return the new query that resulted from adding this operation
     */
    public StackMobOptions select(List<String> fields) {
        headers.add(new Pair<String, String>(SelectHeader, ListHelpers.join(fields, ",")));
        return this;
    }

    /**
     * set the expand depth of this query. the expand depth instructs the StackMob platform to detect relationships and automatically replace those
     * relationship IDs with the values that they point to.
     * @param i the expand depth. at time of writing, StackMob restricts expand depth to maximum 3
     * @return the new query that resulted from adding this operation
     */
    public StackMobOptions expandDepthIs(Integer i) {
        headers.add(new Pair<String, String>(ExpandHeader, i.toString()));
        return this;
    }


    List<Map.Entry<String, String>> getHeaders() {
        return headers;
    }
}
