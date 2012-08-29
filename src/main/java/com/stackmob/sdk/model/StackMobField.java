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

import com.stackmob.sdk.api.StackMobQuery;
import com.stackmob.sdk.api.StackMobQueryWithField;
import com.stackmob.sdk.util.GeoPoint;

import java.util.List;

/**
 * Specifies constraints on a field for use in a query. This is meant to be used as part of {@link StackMobModelQuery} to simplify setting
 * multiple constraints on the same field.
 *
 * <pre>
 * {@code
 * StackMobModelQuery<User> isThirtySomethingQuery2 = new StackMobModelQuery<User>(User.class).field(new StackMobField("age").isGreaterThanOrEqualTo(30).isLessThan(40))
 * }
 * </pre>
 *
 */
public class StackMobField {
    private StackMobQueryWithField q;

    /**
     * create a StackMobField for the given field name
     * @param field the field to constrain
     */
    public StackMobField(String field) {
        q = new StackMobQueryWithField(field, new StackMobQuery(""));
    }

    /**
     * Get the underlying query
     * @return the query
     */
    protected StackMobQuery getQuery() {
        return q.getQuery();
    }

    /**
     * add an equality constraint to the field
     * @param val the value against which to test
     * @return a StackMobField with the new constraint
     */
    public StackMobField isEqualTo(String val) {
        q.isEqualTo(val);
        return this;
    }

    /**
     * add an equality constraint to the field
     * @param val the value against which to test
     * @return a StackMobField with the new constraint
     */
    public StackMobField isEqualTo(Integer val) {
        q.isEqualTo(val);
        return this;
    }

    /**
     * add an equality constraint to the field
     * @param val the value against which to test
     * @return a StackMobField with the new constraint
     */
    public StackMobField isEqualTo(Long val) {
        q.isEqualTo(val);
        return this;
    }

    /**
     * add an equality constraint to the field
     * @param val the value against which to test
     * @return a StackMobField with the new constraint
     */
    public StackMobField isEqualTo(Boolean val) {
        q.isEqualTo(val);
        return this;
    }

    /**
     * add an inequality constraint to the field
     * @param val the value against which to test
     * @return a StackMobField with the new constraint
     */
    public StackMobField isNotEqualTo(String val) {
        q.isNotEqualTo(val);
        return this;
    }

    /**
     * add an inequality constraint to the field
     * @param val the value against which to test
     * @return a StackMobField with the new constraint
     */
    public StackMobField isNotEqualTo(Integer val) {
        q.isNotEqualTo(val);
        return this;
    }

    /**
     * add an inequality constraint to the field
     * @param val the value against which to test
     * @return a StackMobField with the new constraint
     */
    public StackMobField isNotEqualTo(Long val) {
        q.isNotEqualTo(val);
        return this;
    }

    /**
     * add an inequality constraint to the field
     * @param val the value against which to test
     * @return a StackMobField with the new constraint
     */
    public StackMobField isNotEqualTo(Boolean val) {
        q.isNotEqualTo(val);
        return this;
    }

    /**
     * add a constraint that the field is null
     * @return a StackMobField with the new constraint
     */
    public StackMobField isNull() {
        q.isNull();
        return this;
    }

    /**
     * add a constraint that the field is not null
     * @return a StackMobField with the new constraint
     */
    public StackMobField isNotNull() {
        q.isNotNull();
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is near to another point
     * @param point the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isNear(GeoPoint point) {
        q.isNear(point);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within a certain distance of another point
     * @param point the value against which to test
     * @param maxDistanceMi the maximum distance
     * @return a new query with the constraint
     */
    public StackMobField isNearWithinMi(GeoPoint point, Double maxDistanceMi) {
        q.isNearWithinMi(point, maxDistanceMi);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within a certain distance of another point
     * @param point the value against which to test
     * @param maxDistanceKm the maximum distance
     * @return a new query with the constraint
     */
    public StackMobField isNearWithinKm(GeoPoint point, Double maxDistanceKm) {
        q.isNearWithinKm(point, maxDistanceKm);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within a certain distance of another point
     * @param point the value against which to test
     * @param radiusMi the maximum distance
     * @return a new query with the constraint
     */
    public StackMobField isWithinMi(GeoPoint point, Double radiusMi) {
        q.isWithinMi(point, radiusMi);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within a certain distance of another point
     * @param point the value against which to test
     * @param radiusKm the maximum distance
     * @return a new query with the constraint
     */
    public StackMobField isWithinKm(GeoPoint point, Double radiusKm) {
        q.isWithinKm(point, radiusKm);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within the box defined by two other points
     * @param lowerLeft the lower left corner of the box
     * @param upperRight the upper right corner of the box
     * @return a new query with the constraint
     */
    public StackMobField isWithinBox(GeoPoint lowerLeft, GeoPoint upperRight) {
        q.isWithinBox(lowerLeft, upperRight);
        return this;
    }

    /**
     * Constrain the field to be in a set of values
     * @param values the values against which to test
     * @return a new query with the constraint
     */
    public StackMobField isIn(List<String> values) {
        q.isIn(values);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isLessThan(String val) {
        q.isLessThan(val);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isLessThan(Integer val) {
        q.isLessThan(val);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isLessThan(Long val) {
        q.isLessThan(val);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isLessThan(Boolean val) {
        q.isLessThan(val);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isGreaterThan(String val) {
        q.isGreaterThan(val);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isGreaterThan(Integer val) {
        q.isGreaterThan(val);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isGreaterThan(Long val) {
        q.isGreaterThan(val);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isGreaterThan(Boolean val) {
        q.isGreaterThan(val);
        return this;
    }

    /**
     * add a less than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isLessThanOrEqualTo(String val) {
        q.isLessThanOrEqualTo(val);
        return this;
    }

    /**
     * add a less than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isLessThanOrEqualTo(Integer val) {
        q.isLessThanOrEqualTo(val);
        return this;
    }

    /**
     * add a less than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isLessThanOrEqualTo(Long val) {
        q.isLessThanOrEqualTo(val);
        return this;
    }

    /**
     * add a less than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isLessThanOrEqualTo(Boolean val) {
        q.isLessThanOrEqualTo(val);
        return this;
    }

    /**
     * add a greater than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isGreaterThanOrEqualTo(String val) {
        q.isGreaterThanOrEqualTo(val);
        return this;
    }

    /**
     * add a greater than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isGreaterThanOrEqualTo(Integer val) {
        q.isGreaterThanOrEqualTo(val);
        return this;
    }

    /**
     * add a greater than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isGreaterThanOrEqualTo(Long val) {
        q.isGreaterThanOrEqualTo(val);
        return this;
    }

    /**
     * add a greater than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobField isGreaterThanOrEqualTo(Boolean val) {
        q.isGreaterThanOrEqualTo(val);
        return this;
    }

    /**
     * add an ordering on this field
     * @param ordering how results will be ordered using this field
     * @return a new query with the constraint
     */
    public StackMobField isOrderedBy(StackMobQuery.Ordering ordering) {
        q.isOrderedBy(ordering);
        return this;
    }
}
