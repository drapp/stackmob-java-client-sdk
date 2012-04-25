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

import com.stackmob.sdk.api.StackMobQuery;
import com.stackmob.sdk.api.StackMobQueryWithField;
import com.stackmob.sdk.util.GeoPoint;

import java.util.List;

public class StackMobField {
    private StackMobQueryWithField q;
    public StackMobField(String field) {
        q = new StackMobQueryWithField(field, new StackMobQuery(""));
    }

    protected StackMobQuery getQuery() {
        return q.getQuery();
    }

    public StackMobField isEqualTo(String val) {
        q.isEqualTo(val);
        return this;
    }

    public StackMobField isEqualTo(Integer val) {
        q.isEqualTo(val);
        return this;
    }

    public StackMobField isEqualTo(Long val) {
        q.isEqualTo(val);
        return this;
    }

    public StackMobField isEqualTo(Boolean val) {
        q.isEqualTo(val);
        return this;
    }

    public StackMobField isNotEqualTo(String val) {
        q.isNotEqualTo(val);
        return this;
    }

    public StackMobField isNotEqualTo(Integer val) {
        q.isNotEqualTo(val);
        return this;
    }

    public StackMobField isNotEqualTo(Long val) {
        q.isNotEqualTo(val);
        return this;
    }

    public StackMobField isNotEqualTo(Boolean val) {
        q.isNotEqualTo(val);
        return this;
    }

    public StackMobField isNull() {
      q.isNull();
      return this;
    }

    public StackMobField isNotNull() {
      q.isNotNull();
      return this;
    }

    public StackMobField isNear(GeoPoint point) {
        q.isNear(point);
        return this;
    }

    public StackMobField isNearWithinMi(GeoPoint point, Double maxDistanceMi) {
        q.isNearWithinMi(point, maxDistanceMi);
        return this;
    }

    public StackMobField isNearWithinKm(GeoPoint point, Double maxDistanceKm) {
        q.isNearWithinKm(point, maxDistanceKm);
        return this;
    }

    public StackMobField isWithinMi(GeoPoint point, Double radiusMi) {
        q.isWithinMi(point, radiusMi);
        return this;
    }

    public StackMobField isWithinKm(GeoPoint point, Double radiusKm) {
        q.isWithinKm(point, radiusKm);
        return this;
    }

    public StackMobField isWithinBox(GeoPoint lowerLeft, GeoPoint upperRight) {
        q.isWithinBox(lowerLeft, upperRight);
        return this;
    }

    public StackMobField isIn(List<String> values) {
        q.isIn(values);
        return this;
    }

    public StackMobField isLessThan(String val) {
        q.isLessThan(val);
        return this;
    }

    public StackMobField isLessThan(Integer val) {
        q.isLessThan(val);
        return this;
    }

    public StackMobField isLessThan(Long val) {
        q.isLessThan(val);
        return this;
    }

    public StackMobField isLessThan(Boolean val) {
        q.isLessThan(val);
        return this;
    }

    public StackMobField isGreaterThan(String val) {
        q.isGreaterThan(val);
        return this;
    }

    public StackMobField isGreaterThan(Integer val) {
        q.isGreaterThan(val);
        return this;
    }

    public StackMobField isGreaterThan(Long val) {
        q.isGreaterThan(val);
        return this;
    }

    public StackMobField isGreaterThan(Boolean val) {
        q.isGreaterThan(val);
        return this;
    }

    public StackMobField isLessThanOrEqualTo(String val) {
        q.isLessThanOrEqualTo(val);
        return this;
    }

    public StackMobField isLessThanOrEqualTo(Integer val) {
        q.isLessThanOrEqualTo(val);
        return this;
    }

    public StackMobField isLessThanOrEqualTo(Long val) {
        q.isLessThanOrEqualTo(val);
        return this;
    }

    public StackMobField isLessThanOrEqualTo(Boolean val) {
        q.isLessThanOrEqualTo(val);
        return this;
    }

    public StackMobField isGreaterThanOrEqualTo(String val) {
        q.isGreaterThanOrEqualTo(val);
        return this;
    }

    public StackMobField isGreaterThanOrEqualTo(Integer val) {
        q.isGreaterThanOrEqualTo(val);
        return this;
    }

    public StackMobField isGreaterThanOrEqualTo(Long val) {
        q.isGreaterThanOrEqualTo(val);
        return this;
    }

    public StackMobField isGreaterThanOrEqualTo(Boolean val) {
        q.isGreaterThanOrEqualTo(val);
        return this;
    }

    public StackMobField isOrderedBy(StackMobQuery.Ordering ordering) {
        q.isOrderedBy(ordering);
        return this;
    }
}
