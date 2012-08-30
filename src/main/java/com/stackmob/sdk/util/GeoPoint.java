package com.stackmob.sdk.util;

import java.util.*;

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
public class GeoPoint {
  
  private static final double EarthRadiusInMi = 3956.6;
  private static final double EarthRadiusInKm = 6367.5;

  private Double lon = Double.NaN;
  private Double lat = Double.NaN;

  public GeoPoint(Double lon, Double lat) {
    if(lon < -180 || lon > 180 || lat < -90 || lat > 90) {
        throw new IllegalArgumentException("Invalid latitude/longitude. Longitude must be between -180 and 180, while Latitude must be between -90 and 90");
    }
    this.lon = lon;
    this.lat = lat;
  }

  public Double getLongitude() {
    return lon;
  }

  public Double getLatitude() {
    return lat;
  }

  public List<String> asList() {
    List<String> arguments = new ArrayList<String>();
    arguments.add(getLatitude().toString());
    arguments.add(getLongitude().toString());
    return arguments;
  }
  
  public static Double radiansToMi(double radians) {
    return radians * EarthRadiusInMi;
  }
  
  public static Double radiansToKm(double radians) {
    return radians * EarthRadiusInKm;
  }
  
  public static Double miToRadians(double mi) {
    return mi / EarthRadiusInMi;
  }
  
  public static Double kmToRadians(double km) {
    return km / EarthRadiusInKm;
  }
}
