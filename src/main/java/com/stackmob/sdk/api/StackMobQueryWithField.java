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

import java.util.List;

/**
 * Represents a query being executed on a specific field. This is meant to be used as part of a chain with {@link StackMobQuery},
 * and will be generated when you use {@link StackMobQuery}. add the constraints you want and the call {@link #getQuery()}
 * to get a query suitable for passing into a query function.
 * <br />
 *{@code  StackMobQuery query = new StackMobQuery("user").field("age").isGreaterThan(20).isLessThanOrEqualTo(40).field("friend").in(Arrays.asList("joe", "bob", "alice").getQuery();}
 *
 */
public class StackMobQueryWithField {
    private String field;
    private StackMobQuery q;

    /**
     * extend the given query with constraints on the given field
     * @param field
     * @param q
     */
    public StackMobQueryWithField(String field, StackMobQuery q) {
        this.field = field;
        this.q = q;
    }

    /**
     * get the actual query to be used or extended further
     * @return a query with all the constraints set in this object
     */
    public StackMobQuery getQuery() {
        return this.q;
    }

    /**
     * get the field being constrained
     * @return the field
     */
    public String getField() {
        return this.field;
    }

    /**
     * switch to constraints on a new field
     * @param f the field to constrain
     * @return a new query where you can constrain the new field
     */
    public StackMobQueryWithField field(String f) {
        if(this.field.equals(f)) {
            return this;
        }
        else {
            return new StackMobQueryWithField(f, this.q);
        }
    }

    /**
     * add an equality constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isEqualTo(String val) {
        this.q = this.q.fieldIsEqualTo(this.field, val);
        return this;
    }

    /**
     * add an equality constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isEqualTo(Integer val) {
        return this.isEqualTo(val.toString());
    }

    /**
     * add an equality constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isEqualTo(Long val) {
        return this.isEqualTo(val.toString());
    }

    /**
     * add an equality constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isEqualTo(Boolean val) {
        return this.isEqualTo(val.toString());
    }

    /**
     * add an inequality constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNotEqualTo(String val) {
        this.q = this.q.fieldIsNotEqual(this.field, val);
        return this;
    }

    /**
     * add an inequality constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNotEqualTo(Integer val) {
        return this.isNotEqualTo(val.toString());
    }

    /**
     * add an inequality constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNotEqualTo(Long val) {
        return this.isNotEqualTo(val.toString());
    }

    /**
     * add an inequality constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNotEqualTo(Boolean val) {
        return this.isNotEqualTo(val.toString());
    }

    /**
     * add an null constraint to the field
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNull() {
        this.q = this.q.fieldIsNull(this.field);
        return this;
    }

    /**
     * add an not null constraint to the field
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNotNull() {
        this.q = this.q.fieldIsNotNull(this.field);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is near to another point
     * @param point the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNear(GeoPoint point) {
        this.q = this.q.fieldIsNear(this.field, point);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within a certain distance of another point
     * @param point the value against which to test
     * @param maxDistanceMi the maximum distance
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNearWithinMi(GeoPoint point, Double maxDistanceMi) {
        this.q = this.q.fieldIsNearWithinMi(this.field, point, maxDistanceMi);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within a certain distance of another point
     * @param point the value against which to test
     * @param maxDistanceKm the maximum distance
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isNearWithinKm(GeoPoint point, Double maxDistanceKm) {
        this.q = this.q.fieldIsNearWithinKm(this.field, point, maxDistanceKm);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within a certain distance of another point
     * @param point the value against which to test
     * @param radiusMi the maximum distance
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isWithinMi(GeoPoint point, Double radiusMi) {
        this.q = this.q.fieldIsWithinRadiusInMi(this.field, point, radiusMi);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within a certain distance of another point
     * @param point the value against which to test
     * @param radiusKm the maximum distance
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isWithinKm(GeoPoint point, Double radiusKm) {
        this.q = this.q.fieldIsWithinRadiusInKm(this.field, point, radiusKm);
        return this;
    }

    /**
     * add an constraint that this field, which should be a geopoint, is within the box defined by two other points
     * @param lowerLeft the lower left corner of the box
     * @param upperRight the upper right corner of the box
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isWithinBox(GeoPoint lowerLeft, GeoPoint upperRight) {
        this.q = this.q.fieldIsWithinBox(this.field, lowerLeft, upperRight);
        return this;
    }

    /**
     * Constrain the field to be in a set of values
     * @param values the values against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isIn(List<String> values) {
        this.q = this.q.fieldIsIn(this.field, values);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isLessThan(String val) {
        this.q = this.q.fieldIsLessThan(this.field, val);
        return this;
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isLessThan(Integer val) {
        return isLessThan(val.toString());
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isLessThan(Long val) {
        return isLessThan(val.toString());
    }

    /**
     * add a less than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isLessThan(Boolean val) {
        return isLessThan(val.toString());
    }

    /**
     * add a greater than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isGreaterThan(String val) {
        this.q = this.q.fieldIsGreaterThan(this.field, val);
        return this;
    }

    /**
     * add a greater than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isGreaterThan(Integer val) {
        return isGreaterThan(val.toString());
    }

    /**
     * add a greater than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isGreaterThan(Long val) {
        return isGreaterThan(val.toString());
    }

    /**
     * add a greater than constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isGreaterThan(Boolean val) {
        return isGreaterThan(val.toString());
    }

    /**
     * add a less than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isLessThanOrEqualTo(String val) {
        this.q = this.q.fieldIslessThanOrEqualTo(this.field, val);
        return this;
    }

    /**
     * add a less than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isLessThanOrEqualTo(Integer val) {
        return isLessThanOrEqualTo(val.toString());
    }

    /**
     * add a less than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isLessThanOrEqualTo(Long val) {
        return isLessThanOrEqualTo(val.toString());
    }

    /**
     * add a less than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isLessThanOrEqualTo(Boolean val) {
        return isLessThanOrEqualTo(val.toString());
    }

    /**
     * add a greater than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isGreaterThanOrEqualTo(String val) {
        this.q = this.q.fieldIsGreaterThanOrEqualTo(this.field, val);
        return this;
    }

    /**
     * add a greater than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isGreaterThanOrEqualTo(Integer val) {
        return isGreaterThanOrEqualTo(val.toString());
    }

    /**
     * add a greater than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isGreaterThanOrEqualTo(Long val) {
        return isGreaterThanOrEqualTo(val.toString());
    }

    /**
     * add a greater than or equal to constraint to the field
     * @param val the value against which to test
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isGreaterThanOrEqualTo(Boolean val) {
        return isGreaterThanOrEqualTo(val.toString());
    }

    /**
     * add an ordering on this field
     * @param ordering how results will be ordered using this field
     * @return a new query with the constraint
     */
    public StackMobQueryWithField isOrderedBy(StackMobQuery.Ordering ordering) {
        this.q = this.q.fieldIsOrderedBy(this.field, ordering);
        return this;
    }
}