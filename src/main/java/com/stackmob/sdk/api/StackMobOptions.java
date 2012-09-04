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

/**
 * stores the various options that can be passed into a request. At the moment this means select and expand options, as well
 * as arbitrary headers to be passed along with the request
 */
public class StackMobOptions {
    private List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();

    private static final String SelectHeader = "X-StackMob-Select";
    private static final String ExpandHeader = "X-StackMob-Expand";

    /**
     * add a single header to a request
     * @param name the header name
     * @param value the value of the header
     * @return options with the new header set
     */
    public StackMobOptions header(String name, String value) {
        this.headers.add(new Pair<String, String>(name, value));
        return this;
    }

    /**
     * add a set of headers to a request
     * @param headerMap the headers to add
     * @return options with the new headers set
     */
    public StackMobOptions headers(Map<String, String> headerMap) {
        for(Map.Entry<String, String> header: headerMap.entrySet()) {
            this.headers.add(header);
        }
        return this;
    }

    /**
     * add a list of headers to a request
     * @param headers the headers to add
     * @return options with the new headers set
     */
    public StackMobOptions headers(List<Map.Entry<String, String>> headers) {
        this.headers.addAll(headers);
        return this;
    }


    /**
     * restricts the fields returned by a request. This is only supported on get request, login, and getLoggedInUser
     * @param fields the fields to return
     * @return the new query that resulted from adding this operation
     */
    public StackMobOptions select(List<String> fields) {
        headers.add(new Pair<String, String>(SelectHeader, ListHelpers.join(fields, ",")));
        return this;
    }

    /**
     * set the expand depth of objects being returned. Objects with relationships will have their related objects returned as child objects
     * @param i the expand depth, maximum is 3
     * @return the new query that resulted from adding this operation
     */
    public StackMobOptions expandDepthIs(Integer i) {
        if(i > 3) throw new IllegalArgumentException("Maximum expand depth is 3");
        headers.add(new Pair<String, String>(ExpandHeader, i.toString()));
        return this;
    }


    List<Map.Entry<String, String>> getHeaders() {
        return headers;
    }
}
