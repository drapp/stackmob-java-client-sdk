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

public class StackMobQueryWithField {
    private String field;
    private StackMobQuery q;

    public StackMobQueryWithField(String field, StackMobQuery q) {
        this.field = field;
        this.q = q;
    }

    public StackMobQuery getQuery() {
        return this.q;
    }

    public String getField() {
        return this.field;
    }

    public StackMobQueryWithField field(String f) {
        if(this.field.equals(f)) {
            return this;
        }
        else {
            return new StackMobQueryWithField(f, this.q);
        }
    }

    public StackMobQueryWithField isEqualTo(String val) {
        this.q = this.q.fieldIsEqualTo(this.field, val);
        return this;
    }

    public StackMobQueryWithField isEqualTo(Integer val) {
        return this.isEqualTo(val.toString());
    }

    public StackMobQueryWithField isEqualTo(Long val) {
        return this.isEqualTo(val.toString());
    }

    public StackMobQueryWithField isEqualTo(Boolean val) {
        return this.isEqualTo(val.toString());
    }

    public StackMobQueryWithField isNotEqualTo(String val) {
        this.q = this.q.fieldIsNotEqual(this.field, val);
        return this;
    }

    public StackMobQueryWithField isNotEqualTo(Integer val) {
        return this.isNotEqualTo(val.toString());
    }

    public StackMobQueryWithField isNotEqualTo(Long val) {
        return this.isNotEqualTo(val.toString());
    }

    public StackMobQueryWithField isNotEqualTo(Boolean val) {
        return this.isNotEqualTo(val.toString());
    }

    public StackMobQueryWithField isNull() {
        this.q = this.q.fieldIsNull(this.field);
        return this;
    }

    public StackMobQueryWithField isNotNull() {
        this.q = this.q.fieldIsNotNull(this.field);
        return this;
    }

    public StackMobQueryWithField isNear(GeoPoint point) {
        this.q = this.q.fieldIsNear(this.field, point);
        return this;
    }

    public StackMobQueryWithField isNearWithinMi(GeoPoint point, Double maxDistanceMi) {
        this.q = this.q.fieldIsNearWithinMi(this.field, point, maxDistanceMi);
        return this;
    }

    public StackMobQueryWithField isNearWithinKm(GeoPoint point, Double maxDistanceKm) {
        this.q = this.q.fieldIsNearWithinKm(this.field, point, maxDistanceKm);
        return this;
    }

    public StackMobQueryWithField isWithinMi(GeoPoint point, Double radiusMi) {
        this.q = this.q.fieldIsWithinRadiusInMi(this.field, point, radiusMi);
        return this;
    }

    public StackMobQueryWithField isWithinKm(GeoPoint point, Double radiusKm) {
        this.q = this.q.fieldIsWithinRadiusInKm(this.field, point, radiusKm);
        return this;
    }

    public StackMobQueryWithField isWithinBox(GeoPoint lowerLeft, GeoPoint upperRight) {
        this.q = this.q.fieldIsWithinBox(this.field, lowerLeft, upperRight);
        return this;
    }

    public StackMobQueryWithField isIn(List<String> values) {
        this.q = this.q.fieldIsIn(this.field, values);
        return this;
    }

    public StackMobQueryWithField isLessThan(String val) {
        this.q = this.q.fieldIsLessThan(this.field, val);
        return this;
    }

    public StackMobQueryWithField isLessThan(Integer val) {
        return isLessThan(val.toString());
    }

    public StackMobQueryWithField isLessThan(Long val) {
        return isLessThan(val.toString());
    }

    public StackMobQueryWithField isLessThan(Boolean val) {
        return isLessThan(val.toString());
    }

    public StackMobQueryWithField isGreaterThan(String val) {
        this.q = this.q.fieldIsGreaterThan(this.field, val);
        return this;
    }

    public StackMobQueryWithField isGreaterThan(Integer val) {
        return isGreaterThan(val.toString());
    }

    public StackMobQueryWithField isGreaterThan(Long val) {
        return isGreaterThan(val.toString());
    }

    public StackMobQueryWithField isGreaterThan(Boolean val) {
        return isGreaterThan(val.toString());
    }

    public StackMobQueryWithField isLessThanOrEqualTo(String val) {
        this.q = this.q.fieldIslessThanOrEqualTo(this.field, val);
        return this;
    }

    public StackMobQueryWithField isLessThanOrEqualTo(Integer val) {
        return isLessThanOrEqualTo(val.toString());
    }

    public StackMobQueryWithField isLessThanOrEqualTo(Long val) {
        return isLessThanOrEqualTo(val.toString());
    }

    public StackMobQueryWithField isLessThanOrEqualTo(Boolean val) {
        return isLessThanOrEqualTo(val.toString());
    }

    public StackMobQueryWithField isGreaterThanOrEqualTo(String val) {
        this.q = this.q.fieldIsGreaterThanOrEqualTo(this.field, val);
        return this;
    }

    public StackMobQueryWithField isGreaterThanOrEqualTo(Integer val) {
        return isGreaterThanOrEqualTo(val.toString());
    }

    public StackMobQueryWithField isGreaterThanOrEqualTo(Long val) {
        return isGreaterThanOrEqualTo(val.toString());
    }

    public StackMobQueryWithField isGreaterThanOrEqualTo(Boolean val) {
        return isGreaterThanOrEqualTo(val.toString());
    }

    public StackMobQueryWithField isOrderedBy(StackMobQuery.Ordering ordering) {
        this.q = this.q.fieldIsOrderedBy(this.field, ordering);
        return this;
    }
}