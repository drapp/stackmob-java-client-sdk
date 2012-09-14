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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a longitude/latitude pair that can be stored and queried against in StackMob
 */
public class StackMobGeoPoint {

    private static final double EarthRadiusInMi = 3956.6;
    private static final double EarthRadiusInKm = 6367.5;

    private Double lon = Double.NaN;
    private Double lat = Double.NaN;
    private Double distance = null;

    /**
     * create a geopoint in terms of longitude and latitude radian
     * @param lon longitude between +/- 180
     * @param lat latitude between +/- 90
     */
    public StackMobGeoPoint(Double lon, Double lat) {
        if(lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Invalid latitude/longitude. Longitude must be between -180 and 180, while Latitude must be between -90 and 90");
        }
        this.lon = lon;
        this.lat = lat;
    }

    /**
     * the longitude of this geopoint
     * @return the longitude
     */
    public Double getLongitude() {
        return lon;
    }

    /**
     * the latitude of this geopoint
     * @return the latitude
     */
    public Double getLatitude() {
        return lat;
    }


    /**
     * if this GeoPoint came from a "fieldIsNear" query, this will return
     * the distance between this point and the reference point from the query
     * @return the distance between this and the reference point
     */
    public Double getQueryDistanceRadians() {
        return distance;
    }

    /**
     * a list representation of this geopoint
     * @return a list
     */
    public List<String> asList() {
        List<String> arguments = new ArrayList<String>();
        arguments.add(getLatitude().toString());
        arguments.add(getLongitude().toString());
        return arguments;
    }

    /**
     * convert radians on the Earth's surface to miles
     * @param radians input radians
     * @return output miles
     */
    public static Double radiansToMi(double radians) {
        return radians * EarthRadiusInMi;
    }

    /**
     * convert radians on the Earth's surface to kilometers
     * @param radians input radians
     * @return output kilometers
     */
    public static Double radiansToKm(double radians) {
        return radians * EarthRadiusInKm;
    }

    /**
     * convert miles to radians on the Earth's surface
     * @param mi input miles
     * @return output radians
     */
    public static Double miToRadians(double mi) {
        return mi / EarthRadiusInMi;
    }

    /**
     * convert kilometers to radians on the Earth's surface
     * @param km input kilometers
     * @return output radians
     */
    public static Double kmToRadians(double km) {
        return km / EarthRadiusInKm;
    }



}
