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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class was originally adapted from Apache Lucene's GeoHashUtils.java. Please note that
 * this class retains the original licensing (as required), which is different from other classes contained
 * in this project, which are MIT licensed.
 *
 * Relative to the Apache implementation, the code has been cleaned up and expanded.
 * Several new methods have been added to facilitate creating sets of geo hashes for e.g.
 * polygons and other geometric forms.
 */
public class GeoHashUtils {

	private static int DEFAULT_PRECISION = 12;
	private static int[] BITS = { 16, 8, 4, 2, 1 };
	// note: no a,i,l, and o
	private static char[] BASE32_CHARS = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm',
			'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	final static Map<Character, Integer> BASE32_DECODE_MAP = new HashMap<Character, Integer>();
	static {
		for (int i = 0; i < BASE32_CHARS.length; i++) {
			BASE32_DECODE_MAP.put(BASE32_CHARS[i], i);
		}
	}

	private static Object[] encodeWithBbox(double latitude, double longitude, int length) {
		if(length < 1 || length >12) {
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
		return new Object[] {geohash.toString(), new double[] {latInterval[0], latInterval[1], lonInterval[0], lonInterval[1]}};
	}

	/**
	 * Same as encode but returns a substring of the specified length.
	 * @param latitude
	 * @param longitude
	 * @param length
	 * @return geo hash of the specified length. The minimum length is 1 and the maximum length is 12.
	 */
	public static String encode(double latitude, double longitude, int length) {
		if(length < 1 || length >12) {
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
		return geohash.toString();
	}

	/**
	 * Encodes a coordinate into a geo hash.
	 *
	 * @see "http://en.wikipedia.org/wiki/Geohash"
	 * @param latitude
	 * @param longitude
	 * @return geo hash for the coordinate
	 */
	public static String encode(double latitude, double longitude) {
		return encode(latitude, longitude, DEFAULT_PRECISION);
	}

	/**
	 * This decodes the geo hash into it's center. Note that the coordinate that you used to generate
	 * the geo hash may be anywhere in the geo hash's bounding box and therefore you should not
	 * expect them to be identical.
	 *
	 * The original apache code attempted to round the returned coordinate. I have chosen to remove this 'feature' since
	 * it is useful to know the center of the geo hash as exactly as possible, even for very short geo hashes.
	 *
	 * Should you wish to apply some rounding, you can use the GeoGeometry.roundToDecimals method.
	 *
	 * @param geohash
	 * @return a coordinate representing the center of the geohash as a double
	 *         array of [latitude,longitude]
	 */
	public static double[] decode(String geohash) {
		double[] lat_interval = { -90.0, 90.0 };
		double[] lon_interval = { -180.0, 180.0 };

		boolean is_even = true;
		double latitude, longitude;
		for (int i = 0; i < geohash.length(); i++) {
			int currentChar = BASE32_DECODE_MAP.get(geohash.charAt(i));
			for (int z = 0; z < BITS.length; z++) {
				int mask = BITS[z];
				if (is_even) {
					if ((currentChar & mask) != 0) {
						lon_interval[0] = (lon_interval[0] + lon_interval[1]) / 2;
					} else {
						lon_interval[1] = (lon_interval[0] + lon_interval[1]) / 2;
					}

				} else {
					if ((currentChar & mask) != 0) {
						lat_interval[0] = (lat_interval[0] + lat_interval[1]) / 2;
					} else {
						lat_interval[1] = (lat_interval[0] + lat_interval[1]) / 2;
					}
				}
				is_even = is_even ? false : true;
			}

		}
		latitude = (lat_interval[0] + lat_interval[1]) / 2;
		longitude = (lon_interval[0] + lon_interval[1]) / 2;

		return new double[] { latitude, longitude };
	}

	/**
	 * @param geohash
	 * @return double array representing the bounding box for the geohash of
	 *         [nort latitude, south latitude, east longitude, west longitude]
	 */
	public static double[] decode_bbox(String geohash) {
		double[] lat_interval = { -90.0, 90.0 };
		double[] lon_interval = { -180.0, 180.0 };

		boolean is_even = true;
		for (int i = 0; i < geohash.length(); i++) {

			int currentCharacter = BASE32_DECODE_MAP.get(geohash.charAt(i));

			for (int z = 0; z < BITS.length; z++) {
				int mask = BITS[z];
				if (is_even) {
					if ((currentCharacter & mask) != 0) {
						lon_interval[0] = (lon_interval[0] + lon_interval[1]) / 2;
					} else {
						lon_interval[1] = (lon_interval[0] + lon_interval[1]) / 2;
					}

				} else {

					if ((currentCharacter & mask) != 0) {
						lat_interval[0] = (lat_interval[0] + lat_interval[1]) / 2;
					} else {
						lat_interval[1] = (lat_interval[0] + lat_interval[1]) / 2;
					}
				}
				is_even = !is_even;
			}
		}

		return new double[] { lat_interval[0], lat_interval[1],
				lon_interval[0], lon_interval[1] };
	}

	/**
	 * @return the geo hash of the same length directly north of the bounding
	 *         box.
	 */
	public static String north(String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		double latDiff = bbox[1] - bbox[0];
		double lat = bbox[0] - latDiff / 2;
		double lon = (bbox[2] + bbox[3]) / 2;
		return encode(lat, lon).substring(0, geoHash.length());
	}

	/**
	 * @return the geo hash of the same length directly south of the bounding
	 *         box.
	 */
	public static String south(String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		double latDiff = bbox[1] - bbox[0];
		double lat = bbox[1] + latDiff / 2;
		double lon = (bbox[2] + bbox[3]) / 2;
		return encode(lat, lon).substring(0, geoHash.length());
	}

	/**
	 * @return the geo hash of the same length directly west of the bounding
	 *         box.
	 */
	public static String west(String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		double lonDiff = bbox[3] - bbox[2];
		double lat = (bbox[0] + bbox[1]) / 2;
		double lon = bbox[2] - lonDiff / 2;
		return encode(lat, lon).substring(0, geoHash.length());
	}

	/**
	 * @return the geo hash of the same length directly east of the bounding
	 *         box.
	 */
	public static String east(String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		double lonDiff = bbox[3] - bbox[2];
		double lat = (bbox[0] + bbox[1]) / 2;
		double lon = bbox[3] + lonDiff / 2;
		return encode(lat, lon).substring(0, geoHash.length());
	}

	/**
	 * @param geoHash
	 * @param latitude
	 * @param longitude
	 * @return true if the coordinate is contained by the bounding box for this
	 *         geo hash
	 */
	public static boolean contains(String geoHash, double latitude,
			double longitude) {
		return GeoGeometry.bboxContains(decode_bbox(geoHash), latitude,
				longitude);
	}

	/**
	 * @param geoHash
	 * @return a BitSet for the given geoHash
	 */
	public static BitSet toBitSet(String geoHash) {
		BitSet bitSet = new BitSet();
		int b = 0;
		for (int i = 0; i < geoHash.length(); i++) {
			int currentCharacter = BASE32_DECODE_MAP.get(geoHash.charAt(i));
			for (int z = 0; z < BITS.length; z++) {
				if ((currentCharacter & BITS[z]) != 0) {
					bitSet.set(b);
				}
				b++;
			}
		}

		return bitSet;
	}

	/**
	 * @param bitSet
	 * @return a base32 encoded geo hash for a bitset representing a geo hash.
	 */
	public static String fromBitSet(BitSet bitSet) {
		StringBuilder encoded = new StringBuilder();
		int ch = 0;
		int b = 1;
		if (bitSet.length() == 0) {
			return "0";
		}
		for (; b <= bitSet.length(); b++) {
			if (bitSet.get(b - 1)) {
				ch += BITS[(b - 1) % BITS.length];
			}

			if (b % BITS.length == 0 && b != 1) {
				encoded.append(BASE32_CHARS[ch]);
				ch = 0;
			}
		}
		// b will have incremented despite failing the check in the for loop; so
		// compensate
		if ((b - 1) % BITS.length != 0) {
			encoded.append(BASE32_CHARS[ch]);
			ch = 0;
		}
		return encoded.toString();
	}

	/**
	 * Return the 32 geo hashes this geohash can be divided into.
	 *
	 * They are returned alpabetically sorted but in the real world they follow
	 * this pattern:
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
	 * the first 4 share the north east 1/8th the first 8 share the north east
	 * 1/4th the first 16 share the north 1/2 and so on.
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
	 * @param geoHash
	 * @return String array with the geo hashes.
	 */
	public static String[] subHashesNW(String geoHash) {
		ArrayList<String> list = new ArrayList<>();
		for (char c : BASE32_CHARS) {
			if (c >= '0' && c <= '7') {
				list.add(geoHash + c);
			}
		}
		return list.toArray(new String[0]);
	}


	/**
	 * @param geoHash
	 * @return the 16 northern sub hashes of the geo hash
	 */
	public static String[] subHashesN(String geoHash) {
		ArrayList<String> list = new ArrayList<>();
		for (char c : BASE32_CHARS) {
			if (c >= '0' && c <= 'g') {
				list.add(geoHash + c);
			}
		}
		return list.toArray(new String[0]);
	}

	/**
	 * @param geoHash
	 * @return the 16 southern sub hashes of the geo hash
	 */
	public static String[] subHashesS(String geoHash) {
		ArrayList<String> list = new ArrayList<>();
		for (char c : BASE32_CHARS) {
			if (c >= 'h' && c <= 'z') {
				list.add(geoHash + c);
			}
		}
		return list.toArray(new String[0]);
	}

	/**
	 * @param geoHash
	 * @return the 8 north-west sub hashes of the geo hash
	 */
	public static String[] subHashesNE(String geoHash) {
		ArrayList<String> list = new ArrayList<>();
		for (char c : BASE32_CHARS) {
			if (c >= '8' && c <= 'g') {
				list.add(geoHash + c);
			}
		}
		return list.toArray(new String[0]);
	}

	/**
	 * @param geoHash
	 * @return the 8 north-east sub hashes of the geo hash
	 */
	public static String[] subHashesSW(String geoHash) {
		ArrayList<String> list = new ArrayList<>();
		for (char c : BASE32_CHARS) {
			if (c >= 'h' && c <= 'r') {
				list.add(geoHash + c);
			}
		}
		return list.toArray(new String[0]);
	}

	/**
	 * @param geoHash
	 * @return the 8 south-west sub hashes of the geo hash
	 */
	public static String[] subHashesSE(String geoHash) {
		ArrayList<String> list = new ArrayList<>();
		for (char c : BASE32_CHARS) {
			if (c >= 's' && c <= 'z') {
				list.add(geoHash + c);
			}
		}
		return list.toArray(new String[0]);
	}

	/**
	 * @param geoHash
	 * @return the 8 south-east sub hashes of the geo hash
	 */
	public static String[] subHashes(String geoHash) {
		ArrayList<String> list = new ArrayList<>();
		for (char c : BASE32_CHARS) {
			list.add(geoHash + c);
		}
		return list.toArray(new String[0]);
	}

	/**
	 * Cover the polygon with geo hashes. This is useful for indexing mainly.
	 * @param maxLength maximum length of the geoHash; the more you specify, the more expensive it gets
	 * @param polygonPoints polygonPoints points that make up the polygon as arrays of [latitude,longitude]
	 * @return a set of geo hashes that cover the polygon area.
	 */
	public static Set<String> getGeoHashesForPolygon(int maxLength, double[]...polygonPoints) {
		if(maxLength < 2 || maxLength > 10) {
			throw new IllegalArgumentException("maxLength should be between 1 and 10");
		}

		double[] bbox = GeoGeometry.getBbox(polygonPoints);
		// first lets figure out an appropriate geohash length
		double diagonal = GeoGeometry.distance(bbox[0],bbox[2], bbox[1], bbox[3]);
		int hashLength;
		if(diagonal<50) {
			hashLength=8;
		} else if(diagonal<200) {
			hashLength=7;
		} else if(diagonal<1500) {
			hashLength=6;
		} else if(diagonal<10000) {
			hashLength=5;
		} else if(diagonal<50000) {
			hashLength=4;
		} else if(diagonal<200000) {
			hashLength=3;
		} else {
			hashLength=2;
		}

		Set<String> partiallyContained = new HashSet<>();
		// now lets generate all geohashes for the containing bounding box
		// lets start at the top left:
		String rowHash = encode(bbox[0], bbox[2]).substring(0, hashLength);
		double[] rowBox = decode_bbox(rowHash);
		while(rowBox[0]<bbox[1]) {
			String columnHash = rowHash;
			double[] columnBox = rowBox;
			while(columnBox[2]<bbox[3]) {
				partiallyContained.add(columnHash);
				columnHash = east(columnHash);
				columnBox = decode_bbox(columnHash);
			}
			// move to the next row
			rowHash = south(rowHash);
			rowBox = decode_bbox(rowHash);
		}

		Set<String> fullyContained = new TreeSet<>();

		int detail = hashLength;
		// we're not aiming for perfect detail here in terms of 'pixelation', 6 extra chars in the geohash ought to be enough and going beyond 9 doesn't serve much purpose.
		while(detail < maxLength) {
			partiallyContained = splitAndFilter(bbox, fullyContained, partiallyContained);
			detail++;
		}

		// add the remaining hashes that we didn't split
		fullyContained.addAll(partiallyContained);

		return fullyContained;
	}

	private static Set<String> splitAndFilter(double[] bbox,
			Set<String> fullyContained, Set<String> partiallyContained) {
		Set<String> stillPartial = new HashSet<>();
		// now we need to break up the partially contained hashes
		for (String hash : partiallyContained) {
			for (String h : subHashes(hash)) {
				double[] hashBbox = decode_bbox(h);
				boolean nw = GeoGeometry.bboxContains(bbox, hashBbox[0],
						hashBbox[2]);
				boolean ne = GeoGeometry.bboxContains(bbox, hashBbox[0],
						hashBbox[3]);
				boolean sw = GeoGeometry.bboxContains(bbox, hashBbox[1],
						hashBbox[2]);
				boolean se = GeoGeometry.bboxContains(bbox, hashBbox[1],
						hashBbox[3]);
				if (nw && ne && sw && se) {
					fullyContained.add(h);
				} else if (nw || ne || sw || se) {
					stillPartial.add(h);
				} else {
					// ignore it
				}
			}
		}
		return stillPartial;
	}

	/**
	 * @param length
	 * @param wayPoints
	 * @return set of geo hashes along the path with the specified geo hash length
	 */
	public static Set<String> geoHashesForPath(int length, double[]...wayPoints) {
		if(wayPoints == null || wayPoints.length < 2) {
			throw new IllegalArgumentException("must have at least two way points on the path");
		}
		Set<String> hashes = new TreeSet<>();
		// The slope of the line through points A(ax, ay) and B(bx, by) is given by m = (by-ay)/(bx-ax) and the equation of this
		// line can be written y = m(x - ax) + ay.

		for(int i=1;i<wayPoints.length;i++) {
			double[] previousPoint = wayPoints[i-1];
			double[] point = wayPoints[i];
			hashes.addAll(geoHashesForLine(length, previousPoint[0], previousPoint[1], point[0], point[1]));
		}

		return hashes;
	}

	/**
	 * @param length
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return set of geo hashes along the line with the specified geo hash length.
	 */
	public static Set<String> geoHashesForLine(int length, double lat1, double lon1, double lat2, double lon2) {
		if(lat1 == lat2 && lon1 == lon2) {
			throw new IllegalArgumentException("identical begin and end coordinate: line must have two different points");
		}

		double[] bbox1 = (double[]) encodeWithBbox(lat1, lon1, length)[1];
		double[] bbox2 = (double[]) encodeWithBbox(lat2, lon2, length)[1];
		if(lat1 <= lat2) {
			return getGeoHashesForPolygon(length, new double[][] {{bbox1[1],bbox1[2]}, {bbox1[0], bbox1[3]},{bbox2[0],bbox2[3]},{bbox2[1], bbox2[2]}});
		} else {
			return getGeoHashesForPolygon(length, new double[][] {{bbox1[0],bbox1[2]}, {bbox1[1], bbox1[3]},{bbox2[1],bbox2[2]},{bbox2[0], bbox2[3]}});
		}
	}
}
