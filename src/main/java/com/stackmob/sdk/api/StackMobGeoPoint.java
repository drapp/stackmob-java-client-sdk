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

import com.stackmob.sdk.util.GeoPoint;

import java.util.List;

/**
 * Represents a longitude/latitude that can be stored and queried against in StackMob
 */
public class StackMobGeoPoint extends GeoPoint {
    public StackMobGeoPoint(Double lon, Double lat) {
        super(lon, lat);
    }

    //These methods are passed through so we can have nice javadocs in the right place


    /**
     * the longitude of this geopoint
     * @return the longitude
     */
    public Double getLongitude() {
        return super.getLongitude();
    }

    /**
     * the latitude of this geopoint
     * @return the latitude
     */
    public Double getLatitude() {
        return super.getLatitude();
    }

    /**
     * a list representation of this geopoint
     * @return a list
     */
    public List<String> asList() {
        return super.asList();
    }

    /**
     * convert radians on the Earth's surface to miles
     * @param radians input radians
     * @return output miles
     */
    public static Double radiansToMi(double radians) {
        return GeoPoint.radiansToMi(radians);
    }

    /**
     * convert radians on the Earth's surface to kilometers
     * @param radians input radians
     * @return output kilometers
     */
    public static Double radiansToKm(double radians) {
        return GeoPoint.radiansToKm(radians);
    }

    /**
     * convert miles to radians on the Earth's surface
     * @param mi input miles
     * @return output radians
     */
    public static Double miToRadians(double mi) {
        return GeoPoint.miToRadians(mi);
    }

    /**
     * convert kilometers to radians on the Earth's surface
     * @param km input kilometers
     * @return output radians
     */
    public static Double kmToRadians(double km) {
        return GeoPoint.kmToRadians(km);
    }
}
