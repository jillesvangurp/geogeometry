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

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

import java.util.Arrays;
import java.util.Comparator;

public class GeoGeometry {

    /**
     * @param polygonPoints
     *            points that make up the polygon as arrays of
     *            [latitude,longitude]
     * @return bounding box that contains the polygon as a double array of
     *         [minLat,maxLat,minLon,maxLon}
     */
        public static double[] getBbox(double[]... polygonPoints) {
            double minLat = Integer.MAX_VALUE;
            double minLon = Integer.MAX_VALUE;
            double maxLat = Integer.MIN_VALUE;
            double maxLon = Integer.MIN_VALUE;

            for (int i = 0; i < polygonPoints.length; i++) {
                minLat = min(minLat, polygonPoints[i][0]);
                minLon = min(minLon, polygonPoints[i][1]);
                maxLat = max(maxLat, polygonPoints[i][0]);
                maxLon = max(maxLon, polygonPoints[i][1]);
            }

            return new double[] { minLat, maxLat, minLon, maxLon };
        }

    /**
     * @param bbox
     *            double array of [minLat,maxLat,minLon,maxLon}
     * @param latitude
     * @param longitude
     * @return true if the latitude and longitude are contained in the bbox
     */
    public static boolean bboxContains(double[] bbox, double latitude, double longitude) {
        return bbox[0] <= latitude && latitude <= bbox[1] && bbox[2] <= longitude && longitude <= bbox[3];
    }


    public static boolean polygonContains(double[] point, double[]... polygonPoints) {
        return polygonContains(point[0], point[1],polygonPoints);
    }

    /**
     * Determine whether a point is contained in a polygon. Note, technically the points that make up the polygon are not contained by it.
     * @param latitude
     * @param longitude
     * @param polygonPoints
     *            polygonPoints points that make up the polygon as arrays of
     *            [latitude,longitude]
     * @return true if the polygon contains the coordinate
     */
    public static boolean polygonContains(double latitude, double longitude, double[]... polygonPoints) {

        if (polygonPoints.length <= 2) {
            throw new IllegalArgumentException("a polygon must have at least three points");
        }

        double[] bbox = getBbox(polygonPoints);
        if (!bboxContains(bbox, latitude, longitude)) {
            // outside the containing bbox
            return false;
        }

        int hits = 0;

        double lastLatitude = polygonPoints[polygonPoints.length - 1][0];
        double lastLongitude = polygonPoints[polygonPoints.length - 1][1];
        double currentLatitude, currentLongitude;

        // Walk the edges of the polygon
        for (int i = 0; i < polygonPoints.length; lastLatitude = currentLatitude, lastLongitude = currentLongitude, i++) {
            currentLatitude = polygonPoints[i][0];
            currentLongitude = polygonPoints[i][1];

            if (currentLongitude == lastLongitude) {
                continue;
            }

            double leftLatitude;
            if (currentLatitude < lastLatitude) {
                if (latitude >= lastLatitude) {
                    continue;
                }
                leftLatitude = currentLatitude;
            } else {
                if (latitude >= currentLatitude) {
                    continue;
                }
                leftLatitude = lastLatitude;
            }

            double test1, test2;
            if (currentLongitude < lastLongitude) {
                if (longitude < currentLongitude || longitude >= lastLongitude) {
                    continue;
                }
                if (latitude < leftLatitude) {
                    hits++;
                    continue;
                }
                test1 = latitude - currentLatitude;
                test2 = longitude - currentLongitude;
            } else {
                if (longitude < lastLongitude || longitude >= currentLongitude) {
                    continue;
                }
                if (latitude < leftLatitude) {
                    hits++;
                    continue;
                }
                test1 = latitude - lastLatitude;
                test2 = longitude - lastLongitude;
            }

            if (test1 < test2 / (lastLongitude - currentLongitude) * (lastLatitude - currentLatitude)) {
                hits++;
            }
        }

        return (hits & 1) != 0;
    }

    /**
     * Simple rounding method that allows you to get rid of some decimals in a
     * double.
     *
     * @param d
     * @param decimals
     * @return d rounded to the specified precision
     */
    public static double roundToDecimals(double d, int decimals) {
        if (decimals > 17) {
            throw new IllegalArgumentException(
                    "this probably doesn't do what you want; makes sense only for <= 17 decimals");
        }
        double factor = Math.pow(10, decimals);
        return Math.round(d * factor) / factor;
    }

    /**
     * Earth's mean radius, in meters.
     *
     * @see http://en.wikipedia.org/wiki/Earth%27s_radius#Mean_radii
     */
    private static final double EARTH_RADIUS = 6371000.0;

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double EARTH_CIRCUMFERENCE_METERS = EARTH_RADIUS_METERS * Math.PI * 2.0;
    private static final double DEGREE_LATITUDE_METERS = EARTH_RADIUS_METERS * Math.PI / 180.0;

    private static double lengthOfLongitudeDegreeAtLatitude(final double latitude) {
        final double latitudeInRadians = Math.toRadians(latitude);
        return Math.cos(latitudeInRadians) * EARTH_CIRCUMFERENCE_METERS / 360.0;
    }

    /**
     * Translate a point along the longitude for the specified amount of meters.
     * Note, this method assumes the earth is a sphere and the result is not
     * going to be very precise for larger distances.
     *
     * @param latitude
     * @param longitude
     * @param meters
     * @return the translated coordinate.
     */
    public static double[] translateLongitude(double latitude, double longitude, double meters) {
        return new double[] { latitude, longitude + meters / lengthOfLongitudeDegreeAtLatitude(latitude) };
    }

    /**
     * Translate a point along the latitude for the specified amount of meters.
     * Note, this method assumes the earth is a sphere and the result is not
     * going to be very precise for larger distances.
     *
     * @param latitude
     * @param longitude
     * @param meters
     * @return the translated coordinate.
     */
    public static double[] translateLatitude(double latitude, double longitude, double meters) {
        return new double[] { latitude + meters / DEGREE_LATITUDE_METERS, longitude };
    }

    /**
     * Translate a point by the specified meters along the longitude and
     * latitude. Note, this method assumes the earth is a sphere and the result
     * is not going to be very precise for larger distances.
     *
     * @param latitude
     * @param longitude
     * @param lateralMeters
     * @param longitudalMeters
     * @return the translated coordinate.
     */
    public static double[] translate(double latitude, double longitude, double lateralMeters, double longitudalMeters) {
        double[] longitudal = translateLongitude(latitude, longitude, longitudalMeters);
        return translateLatitude(longitudal[0], longitudal[1], lateralMeters);
    }

    /**
     * Compute the Haversine distance between the two coordinates.
     * Haversine is one of several distance calculation algorithms that exist.
     * It is not very precise in the sense that it assumes the earth is a
     * perfect sphere, which it is not. This means precision drops over larger
     * distances. According to http://en.wikipedia.org/wiki/Haversine_formula
     * there is a 0.5% error margin given the 1% difference in curvature between
     * the equator and the poles.
     *
     * @param lat1
     *            the latitude in decimal degrees
     * @param long1
     *            the longitude in decimal degrees
     * @param lat2
     *            the latitude in decimal degrees
     * @param long2
     *            the longitude in decimal degrees
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
     * Variation of the haversine distance method that takes an array representation of a
     * coordinate.
     *
     * @param firstCoordinate
     *            [latitude, longitude]
     * @param secondCoordinate
     *            [latitude, longitude]
     * @return the distance in meters
     */
    public static double distance(double[] firstCoordinate, double[] secondCoordinate) {
        return distance(firstCoordinate[0], firstCoordinate[1], secondCoordinate[0], secondCoordinate[1]);
    }

    /**
     * Simple/naive method for calculating the center of a polygon based on
     * averaging the latitude and longitude. Better algorithms exist but this
     * may be good enough for most purposes.
     *
     * Note, for some polygons, this may actually be located outside the
     * polygon.
     *
     * @param polygonPoints
     *            polygonPoints points that make up the polygon as arrays of
     *            [latitude,longitude]
     * @return the average latitude and longitude.
     */
    public double[] getPolygonCenter(double[]... polygonPoints) {
        double cumLat = 0;
        double cumLon = 0;
        for (double[] coordinate : polygonPoints) {
            cumLat += coordinate[0];
            cumLon += coordinate[1];
        }
        return new double[] { cumLat / polygonPoints.length, cumLon / polygonPoints.length };
    }

    public static double[][] bbox2polygon(double[] bbox) {
        return new double[][] { { bbox[0], bbox[2] }, { bbox[1], bbox[2] }, { bbox[1], bbox[3] }, { bbox[0], bbox[3] } };
    }


    /**
     * Converts a circle to a polygon.
     * @param segments number of segments the polygon should have. The higher this number, the better of an approximation the polygon is for the circle.
     * @param latitude
     * @param longitude
     * @param radius
     * @return an array of the points [latitude,longitude] that make up the polygon.
     */
    public static double[][] circle2polygon(int segments,double latitude, double longitude, double radius) {
        if(segments < 5) {
            throw new IllegalArgumentException("you need a minimum of 5 segments");
        }
        // for n segments you need n+1 points
        double[][] points= new double[segments+1][0];

        double relativeLatitude=radius/EARTH_RADIUS_METERS*180/PI;
        double relativeLongitude=relativeLatitude/cos(Math.toRadians(latitude));

        for(int i=0;i<segments+1;i++) {
            // radians go from 0 to 2*PI; we want to divide the circle in nice segments
            double theta=2*PI*i/segments;

            // on the unit circle, any point of the circle has the coordinate
            // cos(t),sin(t) where t is the radian. So, all we need to do that
            // is multiply that with the relative latitude and longitude
            // note, latitude takes the role of y, not x. By convention we
            // always note latitude, longitude instead of the other way around
            double latOnCircle = latitude + relativeLatitude * Math.sin(theta);
            double lonOnCircle = longitude + relativeLongitude * Math.cos(theta);
            if(lonOnCircle > 180) {
                lonOnCircle = -180 + (lonOnCircle-180);
            } else if(lonOnCircle<-180) {
                lonOnCircle = 180 - (lonOnCircle+180);
            }

            points[i] = new double[]{latOnCircle,lonOnCircle};
        }
        return points;
    }

    /**
     * Calculate a convex polygon containing all the points using the Monotone Chain Convex Hull algorithm
     * @param points
     * @return polygon containing the points.
     */
    @SuppressWarnings({"rawtypes", "unchecked" })
    public static double[][] pointCloudToPolygon(double[]...points) {
        Comparator pointComparator = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                double[] p1 = (double[]) o1;
                double[] p2 = (double[]) o2;
                if(p1[0] == p2[0]) {
                    return (int) Math.round(p1[1]-p2[1]);
                } else {
                    return (int) Math.round(p1[0]-p2[0]);
                }
            }
        };
        double[][] pointsCopy = Arrays.copyOf(points, points.length);
        // sort the copy of points first (don't modify the parameter array)
        Arrays.sort(pointsCopy, pointComparator);

        int n = pointsCopy.length;

        double[][] result = new double[n*2][0];

        // calculate lower hull
        int k=0;
        int start=0;

        for(int i=0; i<n;i++) {
            double[] p = pointsCopy[i];
            while(k-start>=2 && inside(p, result[k-1], result[k-2])) {
                k--;
            }
            result[k++] = p;
        }

        // drop the last one
        k--;
        // calculate upper hull
        start=k;

        for(int i = n-1 ; i >= 0 ; i --) {
            double[] p = pointsCopy[i];
            while(k-start>=2 && inside(p, result[k-1], result[k-2])) {
                k--;
            }
            result[k++] = p;
        }
        // drop the last one
        k--;

        return Arrays.copyOf(result, k);
    }

    private static boolean inside(double[] p, double[] p1, double[] p2) {
        return crossProduct(subtract(p,p1),subtract(p,p2)) > 0;
    }

    private static double[] subtract(double[] p1, double[]p2) {
        return new double[] {p1[0]-p2[0], p1[1]-p2[1]};
    }

    private static double crossProduct(double[] p1, double[]p2) {
        // http://stackoverflow.com/questions/243945/calculating-a-2d-vectors-cross-product
        return p1[0]*p2[1] - p1[1]*p2[0] ;
    }
}
