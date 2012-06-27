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

package com.stackmob.sdk.api;

import com.stackmob.sdk.util.GeoPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that builds queries to execute on the StackMob platform. Example usage:
 * <code>
 *     //this code:
 *     StackMobQuery query = new StackMobQuery("user").field("age").isGreaterThan(20).isLessThanOrEqualTo(40).field("friend").in(Arrays.asList("joe", "bob", "alice").getQuery();
 *     //is identical to this code:
 *     StackMobQuery query = new StackMobQuery("user").fieldIsGreaterThan("user", 20).fieldIsLessThanOrEqualTo("user", 40).fieldIsIn("user", Arrays.asList("joe", "bob", "alice");
 * </code>
 *
 * A few helpful notes about this object:
 * <ul>
 *     <li>this class is not thread safe. make sure to synchronize all calls</li>
 *     <li>calling field("field") on a StackMobQuery will return a StackMobQueryWithField object, which helps you build up part of part of your query on a specific field</li>
 *     <li>
 *         you can chain together operators on a StackMobQueryWithField.
 *         when you're done, call field("field") or getQuery() to get a new StackMobQueryWithField object or the resulting StackMobQuery object (respectively)
 *     </li>
 *     <li>you can only operate on one field at a time, but you can call field("field") as many times as you want on either a StackMobQuery or StackMobQueryWithField object</li>
 *     <li>
 *         you can call methods like fieldIsGreaterThan("field", "value") or fieldIsLessThanOrEqualTo("field", "value") directly on a StackMobQuery object.
 *         the above code sample shows 2 queries that are equivalent. the first line uses StackMobQueryWithField objects, and the second uses direct calls on StackMobQuery
 *     </li>
 * </ul>
 */
public class StackMobQuery {

    private String objectName;
    private Map<String, String> headers = new HashMap<String, String>();
    private Map<String, String> args = new HashMap<String, String>();

    private static final String RangeHeader = "Range";
    private static final String ExpandHeader = "X-StackMob-Expand";
    private static final String OrderByHeader = "X-StackMob-OrderBy";
    private static final String SelectHeader = "X-StackMob-Select";

    public static enum Ordering {
        DESCENDING("desc"),
        ASCENDING("asc");

        private String name;
        Ordering(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public static enum Operator {
        LT("lt"),
        GT("gt"),
        LTE("lte"),
        GTE("gte"),
        IN("in"),
        NEAR("near"),
        WITHIN("within"),
        NE("ne"),
        NULL("null");

        private String operator;

        Operator(String operator) {
            this.operator = operator;
        }

        public String getOperatorForURL() {
            return "["+operator+"]";
        }
    }

    public StackMobQuery() {}

    public StackMobQuery(String objectName) {
        this.objectName = objectName;
    }

    public static StackMobQuery objects(String objectName) {
        return new StackMobQuery(objectName);
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getObjectName() {
        return objectName;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public Map<String, String> getArguments() {
        return this.args;
    }
    
    public StackMobQuery add(StackMobQuery other) {
        this.headers.putAll(other.headers);
        this.args.putAll(other.args);
        return this;
    }

    public StackMobQueryWithField field(String field) {
        return new StackMobQueryWithField(field, this);
    }

    /**
     * add a "NEAR" to your query for the given GeoPoint field. Query results are automatically returned
     * sorted by distance closest to the queried point
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsNear(String field, GeoPoint point) {
        return putInMap(field, Operator.NEAR, join(point.asList()));
    }

    /**
     * add a "NEAR" to your query for the given GeoPoint field. Query results are automatically returned
     * sorted by distance closest to the queried point
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @param maxDistanceMi the maximum distance in miles a matched field can be from point.
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsNearWithinMi(String field, GeoPoint point, Double maxDistanceMi) {
        List<String> arguments = point.asList();
        arguments.add(GeoPoint.miToRadians(maxDistanceMi).toString()); //convert to radians
        return putInMap(field, Operator.NEAR, join(arguments));
    }

    /**
     * add a "NEAR" to your query for the given GeoPoint field. Query results are automatically returned
     * sorted by distance closest to the queried point
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @param maxDistanceKm the maximum distance in kilometers a matched field can be from point.
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsNearWithinKm(String field, GeoPoint point, Double maxDistanceKm) {
        List<String> arguments = point.asList();
        arguments.add(GeoPoint.kmToRadians(maxDistanceKm).toString()); //convert to radians
        return putInMap(field, Operator.NEAR, join(arguments));
    }

    /**
     * add a "WITHIN" to your query for the given GeoPoint field. Query results are not sorted by distance.
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @param radiusInMi the maximum distance in miles a matched field can be from point.
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsWithinRadiusInMi(String field, GeoPoint point, Double radiusInMi) {
        List<String> arguments = point.asList();
        arguments.add(GeoPoint.miToRadians(radiusInMi).toString()); //convert to radians
        return putInMap(field, Operator.WITHIN, join(arguments));
    }

    /**
     * add a "WITHIN" to your query for the given GeoPoint field. Query results are not sorted by distance.
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @param radiusInKm the maximum distance in kilometers a matched field can be from point.
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsWithinRadiusInKm(String field, GeoPoint point, Double radiusInKm) {
        List<String> arguments = point.asList();
        arguments.add(GeoPoint.kmToRadians(radiusInKm).toString()); //convert to radians
        return putInMap(field, Operator.WITHIN, join(arguments));
    }

    /**
     * add a "WITHIN" to your query for the given GeoPoint field. Matched fields will be within the 2-dimensional bounds
     * defined by the lowerLeft and upperRight GeoPoints given
     * @param field the GeoPoint field whose value to test
     * @param lowerLeft the lon/lat location of the lower left corner of the bounding box
     * @param upperRight the lon/lat location of the upper right corner of the bounding box
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsWithinBox(String field, GeoPoint lowerLeft, GeoPoint upperRight) {
        List<String> arguments = lowerLeft.asList();
        arguments.addAll(upperRight.asList());
        return putInMap(field, Operator.WITHIN, join(arguments));
    }

    /**
     * add an "IN" to your query. test whether the given field's value is in the given list of possible values
     * @param field the field whose value to test
     * @param values the values against which to match
     * @return the new query that resulted from adding this operation
     */

    /**
     * add an "IN" to your query. test whether the given field's value is in the given list of possible values
     * @param field the field whose value to test
     * @param values the values against which to match
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsIn(String field, List<String> values) {
        return putInMap(field, Operator.IN, join(values));
    }

    /**
     * add a "NE" to your query. test whether the given field's value is not equal to the given value
     * @param field the field whose value to test
     * @param val the value against which to match
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsNotEqual(String field, String val) {
        return putInMap(field, Operator.NE, val);
    }

    /**
     * add a "NULL" to your query. test whether the given field's value is null
     * @param field the field whose value to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsNull(String field) {
        return putInMap(field, Operator.NULL, "true");
    }

    /**
     * add a "NULL" to your query. test whether the given field's value is not null
     * @param field the field whose value to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsNotNull(String field) {
        return putInMap(field, Operator.NULL, "false");
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except works with Strings
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsLessThan(String field, String val) {
        return putInMap(field, Operator.LT, val);
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies "<=" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIslessThanOrEqualTo(String field, String val) {
        return putInMap(field, Operator.LTE, val);
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies ">" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsGreaterThan(String field, String val) {
        return putInMap(field, Operator.GT, val);
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies ">=" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsGreaterThanOrEqualTo(String field, String val) {
        return putInMap(field, Operator.GTE, val);
    }

    /**
     * add an "=" to your query. test whether the given field's value is equal to the given value
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsEqualTo(String field, String val) {
        args.put(field, val);
        return this;
    }

    /**
     * add an "ORDER BY" to your query
     * @param field the field to order by
     * @param ordering the ordering of that field
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery fieldIsOrderedBy(String field, Ordering ordering) {
        String buf = headers.get(OrderByHeader);
        if(buf != null) {
            buf += ",";
        }
        else {
            buf = "";
        }
        buf += field+":"+ordering.toString();
        headers.put(OrderByHeader, buf);
        return this;
    }

    /**
     * set the expand depth of this query. the expand depth instructs the StackMob platform to detect relationships and automatically replace those
     * relationship IDs with the values that they point to.
     * @param i the expand depth. at time of writing, StackMob restricts expand depth to maximum 3
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery expandDepthIs(Integer i) {
        headers.put(ExpandHeader, i.toString());
        return this;
    }

    /**
     * this method lets you add a "LIMIT" and "SKIP" to your query at once. Can be used to implement pagination in your app.
     * @param start the starting object number (inclusive)
     * @param end the ending object number (inclusive)
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery isInRange(Integer start, Integer end) {
        headers.put(RangeHeader, "objects="+start.toString()+"-"+end.toString());
        return this;
    }

    /**
     * same thing as {@link #isInRange(Integer, Integer)}, except does not specify an end to the range.
     * instead, gets all objects from a starting point (including)
     * @param start the starting object number
     * @return the new query that resulted from adding this operation
     */
    public StackMobQuery isInRange(Integer start) {
        headers.put(RangeHeader, "objects="+start.toString()+"-");
        return this;
    }

    /**
     * restricts the fields returned in the query
     * @param fields the fields to return
     * @return
     */
    public StackMobQuery select(List<String> fields) {
        headers.put(SelectHeader,join(fields));
        return this;
    }

    private StackMobQuery putInMap(String field, Operator operator, String value) {
        args.put(field+operator.getOperatorForURL(), value);
        return this;
    }

    private StackMobQuery putInMap(String field, Operator operator, int value) {
        putInMap(field, operator, Integer.toString(value));
        return this;
    }

    private String join(List<String> values) {
        return join(values, ",");
    }

    private String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        //equivalent of values.join(",");
        boolean first = true;
        for(String val: values) {
            if(!first) {
                builder.append(separator);
            }
            first = false;
            builder.append(val);
        }
        return builder.toString();
    }
}