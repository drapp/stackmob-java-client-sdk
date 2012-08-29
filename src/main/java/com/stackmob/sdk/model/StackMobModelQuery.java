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
package com.stackmob.sdk.model;

import com.google.gson.*;
import com.stackmob.sdk.api.StackMob;
import com.stackmob.sdk.api.StackMobQuery;
import com.stackmob.sdk.callback.StackMobCallback;
import com.stackmob.sdk.callback.StackMobCountCallback;
import com.stackmob.sdk.callback.StackMobQueryCallback;
import com.stackmob.sdk.exception.StackMobException;
import com.stackmob.sdk.util.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Query for a model class on the server based on a number of constraints. This class is used as an argument to {@link StackMobModel#query(Class, com.stackmob.sdk.api.StackMobQuery, com.stackmob.sdk.callback.StackMobQueryCallback)}
 * Each constraint returns a new {@code StackMobModelQuery}, so they can be chained. Use {@link StackMobField} to add mutiple constraints on a field without repeating yourself
 * <pre>
 * {@code
 *
 * // Add constraints
 * StackMobModelQuery<User> isThirtySomethingQuery = new StackMobModelQuery<User>(User.class).fieldIsGreaterThanOrEqualTo("age", 30).fieldIsLessThan("age", 40);
 *
 * // The same using StackMobField
 * StackMobModelQuery<User> isThirtySomethingQuery2 = new StackMobModelQuery<User>(User.class).field(new StackMobField("age").isGreaterThanOrEqualTo(30).isLessThan(40))
 *
 * // Check values in arrays
 * StackMobModelQuery<User> friendsWithBobOrAliceQuery = new StackMobModelQuery<User>(User.class).fieldIsIn("friends", Arrays.asList("bob", "alice"));
 *
 * // Do an actual query
 * User.query(isThirtySomethingQuery, new StackMobQueryCallback<User>() {
 *     public void success(List<User> result) {
 *         // handle success
 *     }
 *
 *     public void failure(StackMobException e) {
 *         // handle failure
 *     }
 * });
 *
 * // Do a count query
 * User.count(isThirtySomethingQuery, new StackMobQueryCallback<User>() {
 *     public void success(List<User> result) {
 *         // handle success
 *     }
 *
 *     public void failure(StackMobException e) {
 *         // handle failure
 *     }
 * });
 * }
 * </pre>
 *
 *
 * @param <T> the subclass of StackMobModel you with to query for
 */
public class StackMobModelQuery<T extends StackMobModel>{

    Class<T> classOfT;
    StackMobQuery query;

    /**
     * Create a new query for the given subclass of StackMobModel
     * @param classOfT because java needs explicit classes passed in everywhere
     */
    public StackMobModelQuery(Class<T> classOfT) {
        this.classOfT = classOfT;
        this.query = new StackMobQuery(this.classOfT.getSimpleName().toLowerCase());
    }

    /**
     * get the underlying StackMobQuery
     * @return the query
     */
    public StackMobQuery getQuery() {
        return query;
    }

    /**
     * send the query with a callback to be invoked with the results
     * @param callback a callback that will be invoked with the results
     */
    public void send(StackMobQueryCallback<T> callback) {
        final StackMobQueryCallback<T> furtherCallback = callback;
        StackMob.getStackMob().get(query, new StackMobCallback() {
            @Override
            public void success(String responseBody) {
                JsonArray array = new JsonParser().parse(responseBody).getAsJsonArray();
                List<T> resultList = new ArrayList<T>();
                for(JsonElement elt : array) {
                    try {
                        resultList.add(StackMobModel.newFromJson(classOfT, elt.toString()));
                    } catch (StackMobException ignore) { }
                }
                furtherCallback.success(resultList);
            }

            @Override
            public void failure(StackMobException e) {
                furtherCallback.failure(e);
            }
        });
    }

    /**
     * send the query, but rather than returning the results just return a count of them
     * @param callback called with the count on success
     */
    public void count(StackMobCountCallback callback) {
        StackMob.getStackMob().count(query, callback);
    }

    /**
     * Add all the constraints specified in this StackMobField object
     * @param fieldObj a field with constraints
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> field(StackMobField fieldObj) {
      query.add(fieldObj.getQuery());
      return this;
    }

    /**
     * add a "NEAR" to your query for the given GeoPoint field. Query results are automatically returned
     * sorted by distance closest to the queried point
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsNear(String field, GeoPoint point) {
        query.fieldIsNear(field, point);
        return this;
    }

    /**
     * add a "NEAR" to your query for the given GeoPoint field. Query results are automatically returned
     * sorted by distance closest to the queried point
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @param maxDistanceMi the maximum distance in miles a matched field can be from point.
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsNearWithinMi(String field, GeoPoint point, Double maxDistanceMi) {
        query.fieldIsNearWithinMi(field, point, maxDistanceMi);
        return this;
    }

    /**
     * add a "NEAR" to your query for the given GeoPoint field. Query results are automatically returned
     * sorted by distance closest to the queried point
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @param maxDistanceKm the maximum distance in kilometers a matched field can be from point.
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsNearWithinKm(String field, GeoPoint point, Double maxDistanceKm) {
        query.fieldIsNearWithinKm(field, point, maxDistanceKm);
        return this;
    }

    /**
     * add a "WITHIN" to your query for the given GeoPoint field. Query results are not sorted by distance.
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @param radiusInMi the maximum distance in miles a matched field can be from point.
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsWithinRadiusInMi(String field, GeoPoint point, Double radiusInMi) {
        query.fieldIsWithinRadiusInMi(field, point, radiusInMi);
        return this;
    }

    /**
     * add a "WITHIN" to your query for the given GeoPoint field. Query results are not sorted by distance.
     * @param field the GeoPoint field whose value to test
     * @param point the lon/lat location to center the search
     * @param radiusInKm the maximum distance in kilometers a matched field can be from point.
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsWithinRadiusInKm(String field, GeoPoint point, Double radiusInKm) {
        query.fieldIsWithinRadiusInKm(field, point, radiusInKm);
        return this;
    }

    /**
     * add a "WITHIN" to your query for the given GeoPoint field. Matched fields will be within the 2-dimensional bounds
     * defined by the lowerLeft and upperRight GeoPoints given
     * @param field the GeoPoint field whose value to test
     * @param lowerLeft the lon/lat location of the lower left corner of the bounding box
     * @param upperRight the lon/lat location of the upper right corner of the bounding box
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsWithinBox(String field, GeoPoint lowerLeft, GeoPoint upperRight) {
        query.fieldIsWithinBox(field, lowerLeft, upperRight);
        return this;
    }

    /**
     * add an "IN" to your query. test whether the given field's value is in the given list of possible values
     * @param field the field whose value to test
     * @param values the values against which to match
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsIn(String field, List<String> values) {
        query.fieldIsIn(field, values);
        return this;
    }

    /**
     * add a "NE" to your query. test whether the given field's value is not equal to the given value
     * @param field the field whose value to test
     * @param val the value against which to match
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsNotEqual(String field, String val) {
        query.fieldIsNotEqual(field, val);
        return this;
    }

    /**
     * add a "NULL" to your query. test whether the given field's value is null
     * @param field the field whose value to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsNull(String field) {
        query.fieldIsNull(field);
        return this;
    }

    /**
     * add a "NULL" to your query. test whether the given field's value is not null
     * @param field the field whose value to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsNotNull(String field) {
        query.fieldIsNotNull(field);
        return this;
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except works with Strings
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsLessThan(String field, String val) {
        query.fieldIsLessThan(field, val);
        return this;
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except works with Strings
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsLessThan(String field, int val) {
        query.fieldIsLessThan(field, val);
        return this;
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies "<=" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsLessThanOrEqualTo(String field, String val) {
        query.fieldIslessThanOrEqualTo(field, val);
        return this;
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies "<=" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsLessThanOrEqualTo(String field, int val) {
        query.fieldIsLessThanOrEqualTo(field, val);
        return this;
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies ">" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsGreaterThan(String field, String val) {
        query.fieldIsGreaterThan(field, val);
        return this;
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies ">" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsGreaterThan(String field, int val) {
        query.fieldIsGreaterThan(field, val);
        return this;
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies ">=" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsGreaterThanOrEqualTo(String field, String val) {
        query.fieldIsGreaterThanOrEqualTo(field, val);
        return this;
    }

    /**
     * same as {@link #fieldIsLessThan(String, String)}, except applies ">=" instead of "<"
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsGreaterThanOrEqualTo(String field, int val) {
        query.fieldIsGreaterThanOrEqualTo(field, val);
        return this;
    }

    /**
     * add an "=" to your query. test whether the given field's value is equal to the given value
     * @param field the field whose value to test
     * @param val the value against which to test
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsEqualTo(String field, String val) {
        query.fieldIsEqualTo(field, val);
        return this;
    }

    /**
     * add an "ORDER BY" to your query
     * @param field the field to order by
     * @param ordering the ordering of that field
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> fieldIsOrderedBy(String field, StackMobQuery.Ordering ordering) {
        query.fieldIsOrderedBy(field, ordering);
        return this;
    }

    /**
     * set the expand depth of this query. the expand depth instructs the StackMob platform to detect relationships and automatically replace those
     * relationship IDs with the values that they point to.
     * @param i the expand depth. at time of writing, StackMob restricts expand depth to maximum 3
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> expandDepthIs(Integer i) {
        query.expandDepthIs(i);
        return this;
    }

    /**
     * this method lets you add a "LIMIT" and "SKIP" to your query at once. Can be used to implement pagination in your app.
     * @param start the starting object number (inclusive)
     * @param end the ending object number (inclusive)
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> isInRange(Integer start, Integer end) {
        query.isInRange(start, end);
        return this;
    }

    /**
     * same thing as {@link #isInRange(Integer, Integer)}, except does not specify an end to the range.
     * instead, gets all objects from a starting point (including)
     * @param start the starting object number
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> isInRange(Integer start) {
        query.isInRange(start);
        return this;
    }

    /**
     * restricts the fields returned in the query
     * @param fields the fields to return
     * @return the new query that resulted from adding this operation
     */
    public StackMobModelQuery<T> select(List<String> fields) {
        query.select(fields);
        return this;
    }

}


