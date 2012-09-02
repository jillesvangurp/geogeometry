/**
 * Copyright (c) 2012, Jilles van Gurp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jillesvangurp.geo;

import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class GeoDistance {

    /**
     * Earth's mean radius, in meters.
     * @see http://en.wikipedia.org/wiki/Earth%27s_radius#Mean_radii
     */
    private static final double EARTH_RADIUS = 6371000.0;

    /**
     * Compute the Haversine distance between the two specified locations.
     *
     * @param lat1
     *        the latitude  in decimal degrees
     * @param long1
     *        the longitude  in decimal degrees
     * @param lat2
     *        the latitude  in decimal degrees
     * @param long2
     *        the longitude  in decimal degrees
     * @return the distance in meters
     */
    public static double distance(final double lat1,
            final double long1, final double lat2, final double long2) {

        final double deltaLat = toRadians(lat2 - lat1);
        final double deltaLon = toRadians(long2 - long1);

        final double a = sin(deltaLat / 2) * sin(deltaLat / 2) + cos(Math.toRadians(lat1))
                * cos(Math.toRadians(lat2)) * sin(deltaLon / 2) * sin(deltaLon / 2);

        final double c = 2 * asin(Math.sqrt(a));

        return EARTH_RADIUS * c;
    }

    /**
     * Variation of the distance method that takes an array representation of a coordinate.
     * @param firstCoordinate [latitude, longitude]
     * @param secondCoordinate [latitude, longitude]
     * @return the distance in meters
     */
    public static double distance(double[] firstCoordinate, double[] secondCoordinate) {
    	return distance(firstCoordinate[0], firstCoordinate[1], secondCoordinate[0], secondCoordinate[1]);
    }
}