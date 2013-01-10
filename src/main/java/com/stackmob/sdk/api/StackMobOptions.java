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
 * Stores the various options that can be passed into a request. Calls can be chained for ease of use
 *
 *
 * <pre>
 * {@code
 * StackMobOptions.https(true).withSelectedFields(Arrays.asList("name", "age").withDepthOf(2)
 * }
 * </pre>
 *
 * Be careful not to call the static methods, which create new options, from non-static context. Java
 * allows this for no good reason with a warning. The object methods are all prefixed with "with".
 * The following code would not do what you want:
 *
 * <pre>
 * {@code
 * // This is wrong! You'll only get the depthOf in the resulting options
 * StackMobOptions.https(true).selectedFields(Arrays.asList("name", "age").depthOf(2)
 * }
 * </pre>
 *
 */
public class StackMobOptions {
    private List<Map.Entry<String, String>> headers = new ArrayList<Map.Entry<String, String>>();
    private List<String> selection = null;
    private int expandDepth = 0;

    private Boolean https = null;
    private static final String SelectHeader = "X-StackMob-Select";
    private static final String ExpandHeader = "X-StackMob-Expand";


    /**
     * empty options that do nothing
     * @return options with nothing set.
     */
    public static StackMobOptions none() {
        return new StackMobOptions();
    }

    /**
     * Force a method to be either http or https, overriding any defaults
     * @param https if true, use https, otherwise http
     * @return the new options with https set
     */
    public static StackMobOptions https(boolean https) {
        return new StackMobOptions().withHTTPS(https);
    }

    /**
     * add a single header to a request
     * @param name the header name
     * @param value the value of the header
     * @return options with the new header set
     */
    public static StackMobOptions header(String name, String value) {
        return none().withHeader(name, value);
    }

    /**
     * add a set of headers to a request
     * @param headerMap the headers to add
     * @return options with the new headers set
     */
    public static StackMobOptions headers(Map<String, String> headerMap) {
        return none().withHeaders(headerMap);
    }

    /**
     * add a list of headers to a request
     * @param headers the headers to add
     * @return options with the new headers set
     */
    public static StackMobOptions headers(List<Map.Entry<String, String>> headers) {
        return none().withHeaders(headers);
    }


    /**
     * restricts the fields returned by a request. This is only supported on get request, login, and getLoggedInUser
     * @param fields the fields to return
     * @return the new query that resulted from adding this operation
     */
    public static StackMobOptions selectedFields(List<String> fields) {
        return none().withSelectedFields(fields);
    }

    /**
     * set the expand depth of objects being returned. Objects with relationships will have their related objects returned as child objects
     * @param depth the expand depth, maximum is 3
     * @return the new query that resulted from adding this operation
     */
    public static StackMobOptions depthOf(int depth) {
        return none().withDepthOf(depth);
    }

    /**
     * Force a method to be either http or https, overriding any defaults or previous settings
     * @param https if true, use https, otherwise http
     * @return the new options with https set
     */
    public StackMobOptions withHTTPS(boolean https) {
        this.https = https;
        return this;
    }

    /**
     * Force a method to be either http or https, overriding any defaults, unless
     * https has already been set
     * @param https if true, use https, otherwise http
     * @return the new options with https set
     */
    public StackMobOptions suggestHTTPS(boolean https) {
        if(this.https == null) this.https = https;
        return this;
    }

    /**
     * add a single header to a request
     * @param name the header name
     * @param value the value of the header
     * @return options with the new header set
     */
    public StackMobOptions withHeader(String name, String value) {
        this.headers.add(new Pair<String, String>(name, value));
        return this;
    }

    /**
     * add a set of headers to a request
     * @param headerMap the headers to add
     * @return options with the new headers set
     */
    public StackMobOptions withHeaders(Map<String, String> headerMap) {
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
    public StackMobOptions withHeaders(List<Map.Entry<String, String>> headers) {
        this.headers.addAll(headers);
        return this;
    }


    /**
     * restricts the fields returned by a request. This is only supported on get request, login, and getLoggedInUser
     * @param fields the fields to return
     * @return the new query that resulted from adding this operation
     */
    public StackMobOptions withSelectedFields(List<String> fields) {
        selection = fields;
        headers.add(new Pair<String, String>(SelectHeader, ListHelpers.join(fields, ",")));
        return this;
    }

    /**
     * set the expand depth of objects being returned. Objects with relationships will have their related objects returned as child objects
     * @param i the expand depth, maximum is 3
     * @return the new query that resulted from adding this operation
     */
    public StackMobOptions withDepthOf(Integer i) {
        if(i > 3) throw new IllegalArgumentException("Maximum expand depth is 3");
        headers.add(new Pair<String, String>(ExpandHeader, i.toString()));
        expandDepth = i;
        return this;
    }


    /**
     * whether or not to use https
     * @return https
     */
    public boolean isHTTPS() {
        return https == null ? false : https;
    }

    /**
     * The headers specified in these options.
     * @return the headers
     */
    public List<Map.Entry<String, String>> getHeaders() {
        return headers;
    }


    /**
     * get the expand depth as set by {@link #withDepthOf(Integer)}
     * @return the expand depth
     */
    public int getExpandDepth() {
        return expandDepth;
    }

    /**
     * get the list of selected fields as specified by {@link #selectedFields(java.util.List)}
     * or {@link #withSelectedFields(java.util.List)}, or null if none specified (meaning all fields
     * are selected).
     * @return the selected fields
     */
    public List<String> getSelection() {
        return selection;
    }
}
