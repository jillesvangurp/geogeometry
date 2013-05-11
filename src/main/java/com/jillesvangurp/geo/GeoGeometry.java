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

import org.apache.commons.lang.Validate;

public class GeoGeometry {

    /**
     * @param polygonPoints
     *            points that make up the polygon as arrays of
     *            [latitude,longitude]
     * @return bounding box that contains the polygon as a double array of
     *         [minLat,maxLat,minLon,maxLon}
     */
    public static double[] boundingBox(double[]... polygonPoints) {
        double minLat = Integer.MAX_VALUE;
        double minLon = Integer.MAX_VALUE;
        double maxLat = Integer.MIN_VALUE;
        double maxLon = Integer.MIN_VALUE;

        for (int i = 0; i < polygonPoints.length; i++) {
            minLat = min(minLat, polygonPoints[i][1]);
            minLon = min(minLon, polygonPoints[i][0]);
            maxLat = max(maxLat, polygonPoints[i][1]);
            maxLon = max(maxLon, polygonPoints[i][0]);
        }

        return new double[] { minLat, maxLat, minLon, maxLon };
    }

    /**
     * Points in a cloud are supposed to be close together. Sometimes bad data causes a handful of points out of
     * thousands to be way off. This method filters those out by sorting the coordinates and then discarding the
     * specified percentage.
     *
     * @param points
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static double[][] filterNoiseFromPointCloud(double[][] points, float percentage) {
        Arrays.sort(points, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                double[] p1 = (double[]) o1;
                double[] p2 = (double[]) o2;
                if(p1[0] == p2[0]) {
                    if(p1[1] > p2[1]) {
                        return 1;
                    } else if(p1[1] == p2[1]){
                        return 0;
                    } else {
                        return -1;
                    }
                } else
                    if(p1[0] > p2[0]) {
                        return 1;
                    } if(p1[0] == p2[0]){
                        return 0;
                    } else {
                        return -1;
                    }
            }
        });
        int discard = (int) (points.length * percentage/2);

        return Arrays.copyOfRange(points, discard, points.length-discard);
    }

    /**
     * @param bbox
     *            double array of [minLat,maxLat,minLon,maxLon}
     * @param latitude
     * @param longitude
     * @return true if the latitude and longitude are contained in the bbox
     */
    public static boolean bboxContains(double[] bbox, double latitude, double longitude) {
        validate(latitude, longitude);
        return bbox[0] <= latitude && latitude <= bbox[1] && bbox[2] <= longitude && longitude <= bbox[3];
    }

    /**
     * Determine whether a point is contained in a polygon. Note, technically
     * the points that make up the polygon are not contained by it.
     *
     * @param point
     * @param polygonPoints 3d array representing a geojson polygon. Note. the polygon holes are ignored currently.
     * @return true if the polygon contains the coordinate
     */
    public static boolean polygonContains(double[] point, double[][][] polygonPoints) {
        validate(point);
        return polygonContains(point[1], point[0], polygonPoints[0]);
    }

    /**
     * Determine whether a point is contained in a polygon. Note, technically
     * the points that make up the polygon are not contained by it.
     *
     * @param point
     * @param polygonPoints
     * @return true if the polygon contains the coordinate
     */
    public static boolean polygonContains(double[] point, double[]... polygonPoints) {
        validate(point);
        return polygonContains(point[1], point[0], polygonPoints);
    }

    /**
     * Determine whether a point is contained in a polygon. Note, technically
     * the points that make up the polygon are not contained by it.
     *
     * @param latitude
     * @param longitude
     * @param polygonPoints 3d array representing a geojson polygon. Note. the polygon holes are ignored currently.
     * @return true if the polygon contains the coordinate
     */
    public static boolean polygonContains(double latitude, double longitude, double[][][] polygonPoints) {
        validate(latitude, longitude);
        return polygonContains(latitude, longitude, polygonPoints[0]);
    }

    /**
     * Determine whether a point is contained in a polygon. Note, technically
     * the points that make up the polygon are not contained by it.
     *
     * @param latitude
     * @param longitude
     * @param polygonPoints
     *            polygonPoints points that make up the polygon as arrays of
     *            [latitude,longitude]
     * @return true if the polygon contains the coordinate
     */
    public static boolean polygonContains(double latitude, double longitude, double[]... polygonPoints) {
        validate(latitude, longitude);

        if (polygonPoints.length <= 2) {
            throw new IllegalArgumentException("a polygon must have at least three points");
        }

        double[] bbox = boundingBox(polygonPoints);
        if (!bboxContains(bbox, latitude, longitude)) {
            // outside the containing bbox
            return false;
        }

        int hits = 0;

        double lastLatitude = polygonPoints[polygonPoints.length - 1][1];
        double lastLongitude = polygonPoints[polygonPoints.length - 1][0];
        double currentLatitude, currentLongitude;

        // Walk the edges of the polygon
        for (int i = 0; i < polygonPoints.length; lastLatitude = currentLatitude, lastLongitude = currentLongitude, i++) {
            currentLatitude = polygonPoints[i][1];
            currentLongitude = polygonPoints[i][0];

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
            throw new IllegalArgumentException("this probably doesn't do what you want; makes sense only for <= 17 decimals");
        }
        double factor = Math.pow(10, decimals);
        return Math.round(d * factor) / factor;
    }

    /**
     * Check if the lines defined by  (x1,y1) (x2,y2) and (u1,v1) (u2,v2) cross each other or not.
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param u1
     * @param v1
     * @param u2
     * @param v2
     * @return true if they cross each other
     */
    public static boolean linesCross(double x1, double y1, double x2, double y2, double u1, double v1, double u2, double v2) {
        // formula for line: y= a+bx

        // vertical lines result in a divide by 0;
        boolean line1Vertical = x2 == x1;
        boolean line2Vertical = u2 == u1;
        if (line1Vertical && line2Vertical) {
            // x=a
            if (x1 == u1) {
                // lines are the same
                return y1 <= v1 && v1 < y2 || y1 <= v2 && v2 < y2;
            } else {
                // parallel -> they don't intersect!
                return false;
            }
        } else if (line1Vertical && !line2Vertical) {
            double b2 = (v2 - v1) / (u2 - u1);
            double a2 = v1 - b2 * u1;

            double xi = x1;
            double yi = a2 + b2 * xi;

            return yi >= y1 && yi <= y2;

        } else if (!line1Vertical && line2Vertical) {
            double b1 = (y2 - y1) / (x2 - x1);
            double a1 = y1 - b1 * x1;

            double xi = u1;
            double yi = a1 + b1 * xi;

            return yi >= v1 && yi <= v2;
        } else {

            double b1 = (y2 - y1) / (x2 - x1);
            // divide by zero if second line vertical
            double b2 = (v2 - v1) / (u2 - u1);

            double a1 = y1 - b1 * x1;
            double a2 = v1 - b2 * u1;

            if (b1 - b2 == 0) {
                if (Math.abs(a1 - a2) < .0000001) {
                    // lines are the same
                    return x1 <= u1 && u1 < x2 || x1 <= u2 && u2 < x2;
                } else {
                    // parallel -> they don't intersect!
                    return false;
                }
            }
            // calculate intersection point xi,yi
            double xi = -(a1 - a2) / (b1 - b2);
            double yi = a1 + b1 * xi;
            if ((x1 - xi) * (xi - x2) >= 0 && (u1 - xi) * (xi - u2) >= 0 && (y1 - yi) * (yi - y2) >= 0 && (v1 - yi) * (yi - v2) >= 0) {
                return true;
            } else {
                return false;
            }
        }
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
        validate(latitude, longitude);
        return new double[] { roundToDecimals(longitude + meters / lengthOfLongitudeDegreeAtLatitude(latitude), 6),latitude };
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
        return new double[] { longitude,roundToDecimals(latitude + meters / DEGREE_LATITUDE_METERS,6) };
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
        validate(latitude, longitude);
        double[] longitudal = translateLongitude(latitude, longitude, longitudalMeters);
        return translateLatitude(longitudal[1], longitudal[0], lateralMeters);
    }

    /**
     * Calculate a bounding box of the specified longitudal and latitudal meters with the latitude/longitude as the center.
     * @param latitude
     * @param longitude
     * @param latitudalMeters
     * @param longitudalMeters
     * @return [minlat,maxlat,minlon,maxlon]
     */
    public static double[] bbox(double latitude, double longitude, double latitudalMeters,double longitudalMeters) {
        validate(latitude, longitude);

        double[] tr = translate(latitude, longitude, latitudalMeters/2, longitudalMeters/2);
        double[] br = translate(latitude, longitude, -latitudalMeters/2, longitudalMeters/2);
        double[] bl = translate(latitude, longitude, -latitudalMeters/2, -longitudalMeters/2);

        return new double[] {tr[1], br[1],tr[0],bl[0]};
    }

    /**
     * Compute the Haversine distance between the two coordinates. Haversine is
     * one of several distance calculation algorithms that exist. It is not very
     * precise in the sense that it assumes the earth is a perfect sphere, which
     * it is not. This means precision drops over larger distances. According to
     * http://en.wikipedia.org/wiki/Haversine_formula there is a 0.5% error
     * margin given the 1% difference in curvature between the equator and the
     * poles.
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
    public static double distance(final double lat1, final double long1, final double lat2, final double long2) {
        validate(lat1, long1);
        validate(lat2, long2);

        final double deltaLat = toRadians(lat2 - lat1);
        final double deltaLon = toRadians(long2 - long1);

        final double a = sin(deltaLat / 2) * sin(deltaLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(deltaLon / 2) * sin(deltaLon / 2);

        final double c = 2 * asin(Math.sqrt(a));

        return EARTH_RADIUS * c;
    }

    /**
     * Variation of the haversine distance method that takes an array
     * representation of a coordinate.
     *
     * @param firstCoordinate
     *            [latitude, longitude]
     * @param secondCoordinate
     *            [latitude, longitude]
     * @return the distance in meters
     */
    public static double distance(double[] firstCoordinate, double[] secondCoordinate) {
        return distance(firstCoordinate[1], firstCoordinate[0], secondCoordinate[1], secondCoordinate[0]);
    }

    /**
     * Calculate distance of a point (pLat,pLon) to a line defined by two other points (lat1,lon1) and (lat2,lon2)
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x
     * @param y
     * @return the distance
     */
    public static double distance(double x1,double y1,double x2, double y2, double x, double y) {
        validate(x1, y1);
        validate(x2, y2);
        validate(x, y);
        double xx,yy;
        if(y1==y2) {
            // horizontal line
            xx=x;
            yy=y1;
        } else if(x1==x2) {
            // vertical line
            xx=x1;
            yy=y;
        } else {
            // y=s*x  +c
            double s= (y2-y1)/(x2-x1);
            double c=y1-s*x1;

            // y=ps*x + pc
            double ps = -1/s;
            double pc=y-ps*x;

            // solve    ps*x +pc = s*x + c
            //          (ps-s) *x = c -pc
            //          x= (c-pc)/(ps-s)
            xx=(c-pc)/(ps-s);
            yy=s*xx+c;

        }
        if(onSegment(xx, yy, x1, y1, x2, y2)) {
            return distance(x,y,xx,yy);
        } else {
            return min(distance(x, y,x1,y1), distance(x,y,x2,y2));
        }
    }

    private static boolean onSegment(double x, double y, double x1, double y1, double x2, double y2) {
        double minx=Math.min(x1, x2);
        double maxx=Math.max(x1, x2);

        double miny=Math.min(y1, y2);
        double maxy=Math.max(y1, y2);


        return x >= minx && x<=maxx && y >= miny && y < maxy;
    }

    /**
     * Calculate distance of a point p to a line defined by two other points l1 and l2.
     * @param l1
     * @param l2
     * @param p
     * @return the distance
     */
    public static double distance(double[] l1, double[] l2, double[] p ) {
        return distance(l1[1],l1[0],l2[1],l2[0],p[1],p[0]);
    }

    public static double distanceToLineString(double[] point, double[][] lineString) {
        if(lineString.length<2) {
            throw new IllegalArgumentException("not enough segments in line");
        }
        double minDistance=Double.MAX_VALUE;
        double[] last=lineString[0];
        for (int i = 1; i < lineString.length; i++) {
            double[] current=lineString[i];
            double distance = distance(last,current,point);
            minDistance = Math.min(minDistance, distance);
            last=current;
        }
        return minDistance;
    }

    /**
     * @param point
     * @param polygon
     * @return distance to polygon
     */
    public static double distanceToPolygon(double[] point, double[][] polygon) {
        if(polygon.length<3) {
            throw new IllegalArgumentException("not enough segments in polygon");
        }
        if(polygonContains(point, polygon)) {
            return 0;
        }
        return distanceToLineString(point, polygon);
    }

    /**
     * @param point
     * @param polygon
     * @return distance to polygon
     */
    public static double distanceToPolygon(double[] point, double[][][] polygon) {
        if(polygon.length==0) {
            throw new IllegalArgumentException("empty polygon");
        }
        return distanceToPolygon(point, polygon[0]);
    }

    /**
     * @param point
     * @param multiPolygon
     * @return distance to the nearest of the polygons in the multipolygon
     */
    public static double distanceToMultiPolygon(double[] point, double[][][][] multiPolygon) {
        double distance = Double.MAX_VALUE;
        for(double[][][] polygon: multiPolygon) {
            distance=Math.min(distance, distanceToPolygon(point, polygon));
        }
        return distance;
    }


    /**
     * Simple/naive method for calculating the center of a polygon based on
     * averaging the latitude and longitude. Better algorithms exist but this
     * may be good enough for most purposes.
     * Note, for some polygons, this may actually be located outside the
     * polygon.
     *
     * @param polygonPoints
     *            polygonPoints points that make up the polygon as arrays of
     *            [longitude,latitude]
     * @return the average longitude and latitude an array.
     */
    public static double[] polygonCenter(double[]... polygonPoints) {
        double cumLon = 0;
        double cumLat = 0;
        for (double[] coordinate : polygonPoints) {
            cumLon += coordinate[0];
            cumLat += coordinate[1];
        }
        return new double[] { cumLon / polygonPoints.length, cumLat / polygonPoints.length };
    }

    public static double[][] bbox2polygon(double[] bbox) {
        return new double[][] { { bbox[2], bbox[0] }, { bbox[2], bbox[1] }, { bbox[3], bbox[1] }, { bbox[3], bbox[0] }, { bbox[2], bbox[0] } };
    }

    /**
     * Converts a circle to a polygon.
     * This method does not behave very well very close to the poles because the math gets a little funny there.
     *
     * @param segments
     *            number of segments the polygon should have. The higher this
     *            number, the better of an approximation the polygon is for the
     *            circle.
     * @param latitude
     * @param longitude
     * @param radius
     * @return an array of the points [longitude,latitude] that make up the
     *         polygon.
     */
    public static double[][] circle2polygon(int segments, double latitude, double longitude, double radius) {
        validate(latitude, longitude);

        if (segments < 5) {
            throw new IllegalArgumentException("you need a minimum of 5 segments");
        }
        double[][] points = new double[segments+1][0];

        double relativeLatitude = radius / EARTH_RADIUS_METERS * 180 / PI;

        // things get funny near the north and south pole, so doing a modulo 90
        // to ensure that the relative amount of degrees doesn't get too crazy.
        double relativeLongitude = relativeLatitude / cos(Math.toRadians(latitude)) % 90;

        for (int i = 0; i < segments; i++) {
            // radians go from 0 to 2*PI; we want to divide the circle in nice
            // segments
            double theta = 2 * PI * i / segments;
            // trying to avoid theta being exact factors of pi because that results in some funny behavior around the
            // north-pole
            theta = theta += 0.1;
            if (theta >= 2 * PI) {
                theta = theta - 2 * PI;
            }

            // on the unit circle, any point of the circle has the coordinate
            // cos(t),sin(t) where t is the radian. So, all we need to do that
            // is multiply that with the relative latitude and longitude
            // note, latitude takes the role of y, not x. By convention we
            // always note latitude, longitude instead of the other way around
            double latOnCircle = latitude + relativeLatitude * Math.sin(theta);
            double lonOnCircle = longitude + relativeLongitude * Math.cos(theta);
            if (lonOnCircle > 180) {
                lonOnCircle = -180 + (lonOnCircle - 180);
            } else if (lonOnCircle < -180) {
                lonOnCircle = 180 - (lonOnCircle + 180);
            }

            if (latOnCircle > 90) {
                latOnCircle = 90 - (latOnCircle - 90);
            } else if (latOnCircle < -90) {
                latOnCircle = -90 - (latOnCircle + 90);
            }

            points[i] = new double[] { lonOnCircle,latOnCircle };
        }
        // should end with same point as the origin
        points[points.length-1] = new double[] {points[0][0],points[0][1]};
        return points;
    }

    /**
     * @param left a 2d array representing a polygon
     * @param right a 2d array representing a polygon
     * @return true if the two polygons overlap
     */
    public static boolean overlap(double[][] left, double[][] right) {
        if(polygonContains(polygonCenter(right), left)) {
            return true;
        }
        if(polygonContains(polygonCenter(left), right)) {
            return true;
        }

        for (double[] p : right) {
            if(polygonContains(p, left)) {
                return true;
            }
        }
        for (double[] p : left) {
            if(polygonContains(p, right)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param containingPolygon
     * @param containedPolygon
     * @return true if the containing polygon fully contains the contained polygon
     */
    public static boolean contains(double[][] containingPolygon, double[][] containedPolygon) {
        for (double[] p : containedPolygon) {
            if(!polygonContains(p, containingPolygon)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to expand the polygon by calculating points around each of the polygon points that are translated the
     * specified amount of meters away. A new polygon is constructed from the resulting point cloud.
     *
     * Given that the contains algorithm disregards polygon points as not contained in the polygon, it is useful to
     * expand the polygon a little if you do require this.
     *
     * @param meters
     * @param points
     * @return a new polygon that fully contains the old polygon and is roughly the specified meters wider.
     */
    public static double[][] expandPolygon(int meters, double[][] points) {
        double[][] expanded = new double[points.length*8][0];
        for (int i = 0; i < points.length; i++) {
            double[] p = points[i];
            double lonPos = translateLongitude(p[0], p[1], meters)[0];
            double lonNeg = translateLongitude(p[0], p[1], -1*meters)[0];
            double latPos = translateLatitude(p[0], p[1], meters)[1];
            double latNeg = translateLatitude(p[0], p[1], -1*meters)[1];
            expanded[i*8]=new double[] {lonPos,latPos};
            expanded[i*8+1]=new double[] {lonPos,latNeg};
            expanded[i*8+2]=new double[] {lonNeg,latPos};
            expanded[i*8+3]=new double[] {lonNeg,latNeg};

            expanded[i*8+4]=new double[] {lonPos,p[1]};
            expanded[i*8+5]=new double[] {lonNeg,p[1]};
            expanded[i*8+6]=new double[] {p[0],latPos};
            expanded[i*8+7]=new double[] {p[1],latNeg};
        }
        return polygonForPoints(expanded);
    }

    /**
     * Calculate a polygon for the specified points.
     * @param points
     * @return a convex polygon for the points
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static double[][] polygonForPoints(double[][] points) {
        if (points.length < 3) {
            throw new IllegalStateException("need at least 3 pois for a polygon");
        }
        double[][] sorted = Arrays.copyOf(points, points.length);
        Arrays.sort(sorted, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                double[] p1 = (double[]) o1;
                double[] p2 = (double[]) o2;

                if (p1[0] == p2[0]) {
                    return (new Double(p1[1]).compareTo(new Double(p2[1])));
                } else {
                    return (new Double(p1[0]).compareTo(new Double(p2[0])));
                }
            }
        });

        int n = sorted.length;

        double[][] lUpper = new double[n][0];

        lUpper[0] = sorted[0];
        lUpper[1] = sorted[1];

        int lUpperSize = 2;

        for (int i = 2; i < n; i++) {
            lUpper[lUpperSize] = sorted[i];
            lUpperSize++;

            while (lUpperSize > 2 && !rightTurn(lUpper[lUpperSize - 3], lUpper[lUpperSize - 2], lUpper[lUpperSize - 1])) {
                // Remove the middle point of the three last
                lUpper[lUpperSize - 2] = lUpper[lUpperSize - 1];
                lUpperSize--;
            }
        }

        double[][] lLower = new double[n][0];

        lLower[0] = sorted[n - 1];
        lLower[1] = sorted[n - 2];

        int lLowerSize = 2;

        for (int i = n - 3; i >= 0; i--) {
            lLower[lLowerSize] = sorted[i];
            lLowerSize++;

            while (lLowerSize > 2 && !rightTurn(lLower[lLowerSize - 3], lLower[lLowerSize - 2], lLower[lLowerSize - 1])) {
                // Remove the middle point of the three last
                lLower[lLowerSize - 2] = lLower[lLowerSize - 1];
                lLowerSize--;
            }
        }

        double[][] result = new double[lUpperSize + lLowerSize-1][0];
        int idx = 0;
        for (int i = 0; i < lUpperSize; i++) {
            result[idx] = (lUpper[i]);
            idx++;
        }

        for (int i = 1; i < lLowerSize-1; i++) {
            // first and last coordinate are also part of lUpper; but polygon should end with itself
            result[idx] = (lLower[i]);
            idx++;
        }
        // close the polygon
        result[result.length-1]=result[0];
        return result;
    }

    /**
     * @param a
     * @param b
     * @param c
     * @return true if b is right of the line defined by a and c
     */
    static boolean rightTurn(double[] a, double[] b, double[] c) {
        return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]) > 0;
    }

    /**
     * @param direction
     *            n,s,e,w
     * @param degrees
     * @param minutes
     * @param seconds
     * @return decimal degree
     */
    public static double toDecimalDegree(String direction, double degrees, double minutes, double seconds) {
        int factor = 1;
        if (direction != null && (direction.toLowerCase().startsWith("w") || direction.toLowerCase().startsWith("s"))) {
            factor = -1;
        }
        return (degrees + minutes / 60 + seconds / 60 / 60) * factor;
    }

    /**
     * @param point
     * @return a json representation of the point
     */
    public static String toJson(double[] point) {
        if(point.length==0) {
            return "[]";
        } else {
            return "["+point[0]+','+point[1]+"]";
        }
    }

    /**
     * @param points
     * @return a json representation of the points
     */
    public static String toJson(double[][] points) {
        StringBuilder buf = new StringBuilder("[");
        for (int i = 0; i < points.length; i++) {
            buf.append(toJson(points[i]));
            if(i<points.length-1) {
                buf.append(',');
            }
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * @param points
     * @return a json representation of the points
     */
    public static String toJson(double[][][] points) {
        StringBuilder buf = new StringBuilder("[");
        for (int i = 0; i < points.length; i++) {
            buf.append(toJson(points[i]));
            if(i<points.length-1) {
                buf.append(',');
            }
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * @param points
     * @return a json representation of the points
     */
    public static String toJson(double[][][][] points) {
        StringBuilder buf = new StringBuilder("[");
        for (int i = 0; i < points.length; i++) {
            buf.append(toJson(points[i]));
            if(i<points.length-1) {
                buf.append(',');
            }
        }
        buf.append("]");
        return buf.toString();
    }

    public static void validate(double latitude, double longitude) {
        if(latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude " + latitude + " is outside legal range of -90,90");
        }
        if(longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude " + longitude + " is outside legal range of -180,180");
        }
    }

    public static void validate(double[] point) {
        validate(point[1],point[0]);
    }


    /**
     * Calculate the approximate area. Like the distance, this is an approximation and you should account for an error
     * of about half a percent.
     *
     * @param polygon
     * @return approximate area.
     */
    public static double area(double[][] polygon) {
        Validate.isTrue(polygon.length > 3,"polygon should have at least three elements");

        double total=0;
        double[] previous=polygon[0];

        double[] center = polygonCenter(polygon);
        double xRef=center[0];
        double yRef=center[1];


        for(int i=1; i< polygon.length;i++) {
            double[] current = polygon[i];
            // convert to cartesian coordinates in meters, note this not very exact
            double x1 = ((previous[0]-xRef)*( 6378137*PI/180 ))*Math.cos( yRef*PI/180 );
            double y1 = (previous[1]-yRef)*( Math.toRadians( 6378137 ) );
            double x2 = ((current[0]-xRef)*( 6378137*PI/180 ))*Math.cos( yRef*PI/180 );
            double y2 = (current[1]-yRef)*( Math.toRadians( 6378137 ) );

            // calculate crossproduct
            total += x1*y2 - x2*y1;
            previous=current;
        }

        return 0.5 * Math.abs(total);
    }

    /**
     * Calculate area of polygon with holes. Assumes geojson style notation where the first 2d array is the outer
     * polygon and the rest represents the holes.
     *
     * @param
     * @return area
     */
    public static double area(double[][][] polygon) {
        Validate.isTrue(polygon.length > 0,"should have at least outer polygon");
        double area = area(polygon[0]);
        for(int i=1;i<polygon.length;i++) {
            // subtract the holes
            area = area - area(polygon[i]);
        }
        return area;
    }

    /**
     * Calculate area of a multi polygon.
     * @param multiPolygon geojson style multi polygon
     * @return area
     */
    public static double area(double[][][][] multiPolygon) {
        double area=0;
        for(int i=0;i<multiPolygon.length;i++) {
            area += area(multiPolygon[i]);
        }
        return area;
    }
}
