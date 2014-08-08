/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Adapted from lucene GeoHashUtils
 */
package com.jillesvangurp.geo;

import static com.jillesvangurp.geo.GeoGeometry.validate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class was originally adapted from Apache Lucene's GeoHashUtils.java. Please note that this class retains the
 * original licensing (as required), which is different from other classes contained in this project, which are MIT
 * licensed.
 *
 * Relative to the Apache implementation, the code has been cleaned up and expanded. Several new methods have been added
 * to facilitate creating sets of geo hashes for e.g. polygons and other geometric forms.
 */
public class GeoHashUtils {

    private static int DEFAULT_PRECISION = 12;
    private static int[] BITS = { 16, 8, 4, 2, 1 };
    // note: no a,i,l, and o
    private static char[] BASE32_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    final static Map<Character, Integer> BASE32_DECODE_MAP = new HashMap<Character, Integer>();
    static {
        for (int i = 0; i < BASE32_CHARS.length; i++) {
            BASE32_DECODE_MAP.put(BASE32_CHARS[i], i);
        }
    }

    /**
     * Same as encode but returns a substring of the specified length.
     *
     * @param latitude latitude
     * @param longitude longitude
     * @param length length in characters (1 to 12)
     * @return geo hash of the specified length. The minimum length is 1 and the maximum length is 12.
     */
    public static String encode(double latitude, double longitude, int length) {
        if (length < 1 || length > 12) {
            throw new IllegalArgumentException("length must be between 1 and 12");
        }
        validate(latitude, longitude, false);
        double[] latInterval = { -90.0, 90.0 };
        double[] lonInterval = { -180.0, 180.0 };

        StringBuilder geohash = new StringBuilder();
        boolean isEven = true;
        int bit = 0, ch = 0;

        while (geohash.length() < length) {
            double mid = 0.0;
            if (isEven) {
                mid = (lonInterval[0] + lonInterval[1]) / 2;
                if (longitude > mid) {
                    ch |= BITS[bit];
                    lonInterval[0] = mid;
                } else {
                    lonInterval[1] = mid;
                }

            } else {
                mid = (latInterval[0] + latInterval[1]) / 2;
                if (latitude > mid) {
                    ch |= BITS[bit];
                    latInterval[0] = mid;
                } else {
                    latInterval[1] = mid;
                }
            }

            isEven = isEven ? false : true;

            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32_CHARS[ch]);
                bit = 0;
                ch = 0;
            }
        }
        return geohash.toString();
    }

    /**
     * Encodes a coordinate into a geo hash.
     *
     * @see "http://en.wikipedia.org/wiki/Geohash"
     * @param latitude latitude
     * @param longitude longitude
     * @return geo hash for the coordinate
     */
    public static String encode(double latitude, double longitude) {
        return encode(latitude, longitude, DEFAULT_PRECISION);
    }

    /**
     * Encode a geojson style point of [longitude,latitude]
     * @param point point
     * @return geohash
     */
    public static String encode(double[] point) {
        return encode(point[1], point[0], DEFAULT_PRECISION);
    }


    /**
     * @param geohash valid geohash
     * @return double array representing the bounding box for the geohash of [south latitude, north latitude, west
     *         longitude, east longitude]
     */
    public static double[] decode_bbox(String geohash) {
        double[] latInterval = { -90.0, 90.0 };
        double[] lonInterval = { -180.0, 180.0 };

        boolean isEven = true;
        for (int i = 0; i < geohash.length(); i++) {

            int currentCharacter = BASE32_DECODE_MAP.get(geohash.charAt(i));

            for (int z = 0; z < BITS.length; z++) {
                int mask = BITS[z];
                if (isEven) {
                    if ((currentCharacter & mask) != 0) {
                        lonInterval[0] = (lonInterval[0] + lonInterval[1]) / 2;
                    } else {
                        lonInterval[1] = (lonInterval[0] + lonInterval[1]) / 2;
                    }

                } else {

                    if ((currentCharacter & mask) != 0) {
                        latInterval[0] = (latInterval[0] + latInterval[1]) / 2;
                    } else {
                        latInterval[1] = (latInterval[0] + latInterval[1]) / 2;
                    }
                }
                isEven = !isEven;
            }
        }

        return new double[] { latInterval[0], latInterval[1], lonInterval[0], lonInterval[1] };
    }

    /**
     * This decodes the geo hash into it's center. Note that the coordinate that you used to generate the geo hash may
     * be anywhere in the geo hash's bounding box and therefore you should not expect them to be identical.
     *
     * The original apache code attempted to round the returned coordinate. I have chosen to remove this 'feature' since
     * it is useful to know the center of the geo hash as exactly as possible, even for very short geo hashes.
     *
     * Should you wish to apply some rounding, you can use the GeoGeometry.roundToDecimals method.
     *
     * @param geohash valid geohash
     * @return a coordinate representing the center of the geohash as a double array of [longitude,latitude]
     */
    public static double[] decode(String geohash) {
        double[] bbox = decode_bbox(geohash);

        double latitude = (bbox[0] + bbox[1]) / 2;
        double longitude = (bbox[2] + bbox[3]) / 2;

        return new double[] { longitude, latitude };
    }

    /**
     * @param geoHash geohash
     * @return the geo hash of the same length directly south of the bounding box.
     */
    public static String south(String geoHash) {
        double[] bbox = decode_bbox(geoHash);
        double latDiff = bbox[1] - bbox[0];
        double lat = bbox[0] - latDiff / 2;
        double lon = (bbox[2] + bbox[3]) / 2;
        return encode(lat, lon, geoHash.length());
    }

    /**
     * @param geoHash geohash
     * @return the geo hash of the same length directly north of the bounding box.
     */
    public static String north(String geoHash) {
        double[] bbox = decode_bbox(geoHash);
        double latDiff = bbox[1] - bbox[0];
        double lat = bbox[1] + latDiff / 2;
        double lon = (bbox[2] + bbox[3]) / 2;
        return encode(lat, lon, geoHash.length());
    }

    /**
     * @param geoHash geohash
     * @return the geo hash of the same length directly west of the bounding box.
     */
    public static String west(String geoHash) {
        double[] bbox = decode_bbox(geoHash);
        double lonDiff = bbox[3] - bbox[2];
        double lat = (bbox[0] + bbox[1]) / 2;
        double lon = bbox[2] - lonDiff / 2;
        if (lon < -180) {
            lon = 180 - (lon + 180);
        }
        if(lon > 180) {
            lon=180;
        }

        return encode(lat, lon, geoHash.length());
    }

    /**
     * @param geoHash geohash
     * @return the geo hash of the same length directly east of the bounding box.
     */
    public static String east(String geoHash) {
        double[] bbox = decode_bbox(geoHash);
        double lonDiff = bbox[3] - bbox[2];
        double lat = (bbox[0] + bbox[1]) / 2;
        double lon = bbox[3] + lonDiff / 2;

        if (lon > 180) {
            lon = -180 + (lon - 180);
        }
        if(lon<-180) {
            lon=-180;
        }

        return encode(lat, lon, geoHash.length());
    }

    /**
     * @param geoHash geo hash
     * @param latitude latitude
     * @param longitude longitude
     * @return true if the coordinate is contained by the bounding box for this geo hash
     */
    public static boolean contains(String geoHash, double latitude, double longitude) {
        return GeoGeometry.bboxContains(decode_bbox(geoHash), latitude, longitude);
    }

    /**
     * Return the 32 geo hashes this geohash can be divided into.
     *
     * They are returned alpabetically sorted but in the real world they follow this pattern:
     *
     * <pre>
     * u33dbfc0 u33dbfc2 | u33dbfc8 u33dbfcb
     * u33dbfc1 u33dbfc3 | u33dbfc9 u33dbfcc
     * -------------------------------------
     * u33dbfc4 u33dbfc6 | u33dbfcd u33dbfcf
     * u33dbfc5 u33dbfc7 | u33dbfce u33dbfcg
     * -------------------------------------
     * u33dbfch u33dbfck | u33dbfcs u33dbfcu
     * u33dbfcj u33dbfcm | u33dbfct u33dbfcv
     * -------------------------------------
     * u33dbfcn u33dbfcq | u33dbfcw u33dbfcy
     * u33dbfcp u33dbfcr | u33dbfcx u33dbfcz
     * </pre>
     *
     * the first 4 share the north east 1/8th the first 8 share the north east 1/4th the first 16 share the north 1/2
     * and so on.
     *
     * They are ordered as follows:
     *
     * <pre>
     *  0  2  8 10
     *  1  3  9 11
     *  4  6 12 14
     *  5  7 13 15
     * 16 18 24 26
     * 17 19 25 27
     * 20 22 28 30
     * 21 23 29 31
     * </pre>
     *
     * Some useful properties: Anything ending with
     *
     * <pre>
     * 0-g = N
     * h-z = S
     *
     * 0-7 = NW
     * 8-g = NE
     * h-r = SW
     * s-z = SE
     * </pre>
     *
     * @param geoHash geo hash
     * @return String array with the geo hashes.
     */

    public static String[] subHashes(String geoHash) {
        ArrayList<String> list = new ArrayList<String>();
        for (char c : BASE32_CHARS) {
            list.add(geoHash + c);
        }
        return list.toArray(new String[0]);
    }

    /**
     * 2d array with the 32 possible geohash endings layed out as they would if you would break down a geohash into
     * its subhashes.
     */
    public static final char[][] GEOHASH_ENDINGS = new char[][]{
        {'0','2','8','b'},
        {'1','3','9','c'},
        {'4','6','d','f'},
        {'5','7','e','g'},
        {'h','k','s','u'},
        {'j','m','t','v'},
        {'n','q','w','y'},
        {'p','r','x','z'}
    };

    /**
     * @param geoHash geo hash
     * @return the 16 northern sub hashes of the geo hash
     */
    public static String[] subHashesN(String geoHash) {
        ArrayList<String> list = new ArrayList<String>();
        for (char c : BASE32_CHARS) {
            if (c >= '0' && c <= 'g') {
                list.add(geoHash + c);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * @param geoHash geo hash
     * @return the 16 southern sub hashes of the geo hash
     */
    public static String[] subHashesS(String geoHash) {
        ArrayList<String> list = new ArrayList<String>();
        for (char c : BASE32_CHARS) {
            if (c >= 'h' && c <= 'z') {
                list.add(geoHash + c);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * @param geoHash geo hash
     * @return the 8 north-west sub hashes of the geo hash
     */
    public static String[] subHashesNW(String geoHash) {
        ArrayList<String> list = new ArrayList<String>();
        for (char c : BASE32_CHARS) {
            if (c >= '0' && c <= '7') {
                list.add(geoHash + c);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * @param geoHash geo hash
     * @return the 8 north-east sub hashes of the geo hash
     */
    public static String[] subHashesNE(String geoHash) {
        ArrayList<String> list = new ArrayList<String>();
        for (char c : BASE32_CHARS) {
            if (c >= '8' && c <= 'g') {
                list.add(geoHash + c);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * @param geoHash geo hash
     * @return the 8 south-west sub hashes of the geo hash
     */
    public static String[] subHashesSW(String geoHash) {
        ArrayList<String> list = new ArrayList<String>();
        for (char c : BASE32_CHARS) {
            if (c >= 'h' && c <= 'r') {
                list.add(geoHash + c);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * @param geoHash geo hash
     * @return the 8 south-east sub hashes of the geo hash
     */
    public static String[] subHashesSE(String geoHash) {
        ArrayList<String> list = new ArrayList<String>();
        for (char c : BASE32_CHARS) {
            if (c >= 's' && c <= 'z') {
                list.add(geoHash + c);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * Cover the polygon with geo hashes. Calls getGeoHashesForPolygon(int maxLength, double[]... polygonPoints) with a
     * maxLength that is the suitable hashlength for the surrounding bounding box + 1. If you need more fine grained
     * boxes, specify your own maxLength.
     *
     * Note, the algorithm 'fills' the polygon from the inside with hashes. So, if a geohash partially falls outside the
     * polygon, it is omitted. So, if you have a polygon with a lot of detail, this may result in large portions not
     * being covered. To resolve this, manually choose a bigger geohash length. This results, in more but smaller
     * geohashes around the edges.
     *
     * The algorithm works for both convex and concave algorithms.
     *
     * @param polygonPoints linestring
     *            2d array of polygonPoints points that make up the polygon as arrays of [longitude, latitude]
     * @return a set of geo hashes that cover the polygon area.
     */
    public static Set<String> geoHashesForPolygon(double[]... polygonPoints) {
        double[] bbox = GeoGeometry.boundingBox(polygonPoints);
        // first lets figure out an appropriate geohash length
        double diagonal = GeoGeometry.distance(bbox[0], bbox[2], bbox[1], bbox[3]);
        int hashLength = suitableHashLength(diagonal, bbox[0], bbox[2]);
        return geoHashesForPolygon(hashLength + 1, polygonPoints);
    }

    /**
     * Cover the polygon with geo hashes. Calls getGeoHashesForPolygon(int maxLength, double[]... polygonPoints) with a
     * maxLength that is the suitable hashlength for the surrounding bounding box + 1. If you need more fine grained
     * boxes, specify your own maxLength.
     *
     * Note, the algorithm 'fills' the polygon from the inside with hashes. So, if a geohash partially falls outside the
     * polygon, it is omitted. So, if you have a polygon with a lot of detail, this may result in large portions not
     * being covered. To resolve this, manually choose a bigger geohash length. This results, in more but smaller
     * geohashes around the edges.
     *
     * The algorithm works for both convex and concave algorithms.
     *
     * @param maxLength
     *            maximum length of the geoHash; the more you specify, the more expensive it gets
     * @param polygonPoints
     *            2d array of polygonPoints points that make up the polygon as arrays of [longitude, latitude]
     * @return a set of geo hashes that cover the polygon area.
     */
    public static Set<String> geoHashesForPolygon(int maxLength, double[]... polygonPoints) {
        for (double[] ds : polygonPoints) {
            // basically the algorithm can go into an endless loop. Best to avoid the poles.
            if(ds[1] < -89.5 || ds[1] > 89.5) {
                throw new IllegalArgumentException(
                        "please stay away from the north pole or the south pole; there are some known issues there. Besides, nothing there but snow and ice.");
            }
        }
        if (maxLength < 1 || maxLength >= DEFAULT_PRECISION) {
            throw new IllegalArgumentException("maxLength should be between 2 and " + DEFAULT_PRECISION + " was " + maxLength);
        }

        double[] bbox = GeoGeometry.boundingBox(polygonPoints);
        // first lets figure out an appropriate geohash length
        double diagonal = GeoGeometry.distance(bbox[0], bbox[2], bbox[1], bbox[3]);
        int hashLength = suitableHashLength(diagonal, bbox[0], bbox[2]);

        Set<String> partiallyContained = new HashSet<String>();
        // now lets generate all geohashes for the containing bounding box
        // lets start at the top left:

        String rowHash = encode(bbox[0], bbox[2], hashLength);
        double[] rowBox = decode_bbox(rowHash);
        while (rowBox[0] < bbox[1]) {
            String columnHash = rowHash;
            double[] columnBox = rowBox;

            while (isWest(columnBox[2], bbox[3])) {
                partiallyContained.add(columnHash);
                columnHash = east(columnHash);
                columnBox = decode_bbox(columnHash);
            }

            // move to the next row
            rowHash = north(rowHash);
            rowBox = decode_bbox(rowHash);
        }

        Set<String> fullyContained = new TreeSet<String>();

        int detail = hashLength;
        // we're not aiming for perfect detail here in terms of 'pixellation', 6
        // extra chars in the geohash ought to be enough and going beyond 9
        // doesn't serve much purpose.
        while (detail < maxLength) {
            partiallyContained = splitAndFilter(polygonPoints, fullyContained, partiallyContained);
            detail++;
        }
        if (fullyContained.size() == 0) {
            fullyContained.addAll(partiallyContained);
        }

        return fullyContained;
    }

    /**
     * @param l1 longitude
     * @param l2 longitude
     * @return true if l1 is west of l2
     */
    public static boolean isWest(double l1, double l2) {
        double ll1 = l1 + 180;
        double ll2 = l2 + 180;
        if (ll1 < ll2 && ll2 - ll1 < 180) {
            return true;
        } else if (ll1 > ll2 && ll2 + 360 - ll1 < 180) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param l1 longitude
     * @param l2 longitude
     * @return true if l1 is east of l2
     */
    public static boolean isEast(double l1, double l2) {
        double ll1 = l1 + 180;
        double ll2 = l2 + 180;
        if (ll1 > ll2 && ll1 - ll2 < 180) {
            return true;
        } else if (ll1 < ll2 && ll1 + 360 - ll2 < 180) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param l1 latitude
     * @param l2 latitude
     * @return true if l1 is north of l2
     */
    public static boolean isNorth(double l1, double l2) {
        return l1 > l2;
    }

    /**
     * @param l1 latitude
     * @param l2 latitude
     * @return true if l1 is south of l2
     */
    public static boolean isSouth(double l1, double l2) {
        return l1 < l2;
    }

    @SuppressWarnings("deprecation")
    private static Set<String> splitAndFilter(double[][] polygonPoints, Set<String> fullyContained, Set<String> partiallyContained) {
        Set<String> stillPartial = new HashSet<String>();
        // now we need to break up the partially contained hashes
        for (String hash : partiallyContained) {
            for (String h : subHashes(hash)) {
                double[] hashBbox = decode_bbox(h);
                boolean nw = GeoGeometry.polygonContains(new double[] { hashBbox[2], hashBbox[0] }, polygonPoints);
                boolean ne = GeoGeometry.polygonContains(new double[] { hashBbox[3], hashBbox[0] }, polygonPoints);
                boolean sw = GeoGeometry.polygonContains(new double[] { hashBbox[2], hashBbox[1] }, polygonPoints);
                boolean se = GeoGeometry.polygonContains(new double[] { hashBbox[3], hashBbox[1] }, polygonPoints);
                if (nw && ne && sw && se) {
                    fullyContained.add(h);
                } else if (nw || ne || sw || se) {
                    stillPartial.add(h);
                } else {
                    double[] last = polygonPoints[0];
                    for (int i = 1; i < polygonPoints.length; i++) {
                        double[] current = polygonPoints[i];
                        if (GeoGeometry.linesCross(hashBbox[0], hashBbox[2], hashBbox[0], hashBbox[3], last[1], last[0], current[1], current[0])) {
                            stillPartial.add(h);
                            break;
                        } else if (GeoGeometry.linesCross(hashBbox[0], hashBbox[3], hashBbox[1], hashBbox[3], last[1], last[0], current[1], current[0])) {
                            stillPartial.add(h);
                            break;
                        } else if (GeoGeometry.linesCross(hashBbox[1], hashBbox[3], hashBbox[1], hashBbox[2], last[1], last[0], current[1], current[0])) {
                            stillPartial.add(h);
                            break;
                        } else if (GeoGeometry.linesCross(hashBbox[1], hashBbox[2], hashBbox[0], hashBbox[2], last[1], last[0], current[1], current[0])) {
                            stillPartial.add(h);
                            break;
                        }
                    }
                }
            }
        }

        return stillPartial;
    }

    /**
     * @param hashLength desired length of the geohash
     * @param wayPoints line string
     * @return set of geo hashes along the path with the specified geo hash length
     */
    public static Set<String> geoHashesForPath(int hashLength, double[]... wayPoints) {
        if (wayPoints == null || wayPoints.length < 2) {
            throw new IllegalArgumentException("must have at least two way points on the path");
        }
        Set<String> hashes = new TreeSet<String>();
        // The slope of the line through points A(ax, ay) and B(bx, by) is given
        // by m = (by-ay)/(bx-ax) and the equation of this
        // line can be written y = m(x - ax) + ay.

        for (int i = 1; i < wayPoints.length; i++) {
            double[] previousPoint = wayPoints[i - 1];
            double[] point = wayPoints[i];
            hashes.addAll(geoHashesForLine(hashLength, previousPoint[0], previousPoint[1], point[0], point[1]));
        }

        return hashes;
    }

    /**
     * @param width in meters
     * @param lat1 latitude
     * @param lon1 longitude
     * @param lat2 latitude
     * @param lon2 longitude
     * @return set of geo hashes along the line with the specified geo hash length.
     */
    public static Set<String> geoHashesForLine(double width, double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) {
            throw new IllegalArgumentException("identical begin and end coordinate: line must have two different points");
        }

        int hashLength = suitableHashLength(width, lat1, lon1);

        Object[] result1 = encodeWithBbox(lat1, lon1, hashLength);
        double[] bbox1 = (double[]) result1[1];

        Object[] result2 = encodeWithBbox(lat2, lon2, hashLength);
        double[] bbox2 = (double[]) result2[1];

        if (result1[0].equals(result2[0])) { // same geohash for begin and end
            HashSet<String> results = new HashSet<String>();
            results.add((String) result1[0]);
            return results;
        } else if (lat1 != lat2) {
            return geoHashesForPolygon(hashLength, new double[][] { { bbox1[0], bbox1[2] }, { bbox1[1], bbox1[2] }, { bbox2[1], bbox2[3] },
                    { bbox2[0], bbox2[3] } });
        } else {
            return geoHashesForPolygon(hashLength, new double[][] { { bbox1[0], bbox1[2] }, { bbox1[0], bbox1[3] }, { bbox2[1], bbox2[2] },
                    { bbox2[1], bbox2[3] } });
        }
    }

    private static Object[] encodeWithBbox(double latitude, double longitude, int length) {
        if (length < 1 || length > 12) {
            throw new IllegalArgumentException("length must be between 1 and 12");
        }
        double[] latInterval = { -90.0, 90.0 };
        double[] lonInterval = { -180.0, 180.0 };

        StringBuilder geohash = new StringBuilder();
        boolean is_even = true;
        int bit = 0, ch = 0;

        while (geohash.length() < length) {
            double mid = 0.0;
            if (is_even) {
                mid = (lonInterval[0] + lonInterval[1]) / 2;
                if (longitude > mid) {
                    ch |= BITS[bit];
                    lonInterval[0] = mid;
                } else {
                    lonInterval[1] = mid;
                }

            } else {
                mid = (latInterval[0] + latInterval[1]) / 2;
                if (latitude > mid) {
                    ch |= BITS[bit];
                    latInterval[0] = mid;
                } else {
                    latInterval[1] = mid;
                }
            }

            is_even = is_even ? false : true;

            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32_CHARS[ch]);
                bit = 0;
                ch = 0;
            }
        }
        return new Object[] { geohash.toString(), new double[] { latInterval[0], latInterval[1], lonInterval[0], lonInterval[1] } };
    }

    /**
     * @param length geohash length
     * @param latitude latitude
     * @param longitude longitude
     * @param radius radius in meters
     * @return set of geohashes
     */
    public static Set<String> geoHashesForCircle(int length, double latitude, double longitude, double radius) {
        // bit of a wet finger approach here: it doesn't make much sense to have
        // lots of segments unless we have a long geohash or a large radius
        int segments;
        int suitableHashLength = suitableHashLength(radius, latitude, longitude);
        if (length > suitableHashLength - 3) {
            segments = 200;
        } else if (length > suitableHashLength - 2) {
            segments = 100;
        } else if (length > suitableHashLength - 1) {
            segments = 50;
        } else {
            // we don't seem to care about detail
            segments = 15;
        }

        double[][] circle2polygon = GeoGeometry.circle2polygon(segments, latitude, longitude, radius);
        return geoHashesForPolygon(length, circle2polygon);
    }

    /**
     * @param granularityInMeters granularity
     * @param latitude latitude
     * @param longitude longitude
     * @return the largest hash length where the hash bbox has a width less than granularityInMeters.
     */
    public static int suitableHashLength(double granularityInMeters, double latitude, double longitude) {
        if (granularityInMeters < 5) {
            return 10;
        }
        String hash = encode(latitude, longitude);
        double width = 0;
        int length = hash.length();
        // the height is the same at for any latitude given a length, but the width converges towards the poles
        while (width < granularityInMeters && hash.length() >= 2) {
            length = hash.length();
            double[] bbox = decode_bbox(hash);
            width = GeoGeometry.distance(bbox[0], bbox[2], bbox[0], bbox[3]);
            hash = hash.substring(0, hash.length() - 1);
        }

        return Math.min(length + 1, DEFAULT_PRECISION);
    }
}
