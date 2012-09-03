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
import java.util.Map;

/**
 * This class was adapted from Apache Lucene's GeoHashUtils. Please note that
 * this class retains the original licensing.
 *
 * Relative to the Apache implementation, I have cleaned up some of the code and
 * added a few useful methods that make it easier to work with geo hashes.
 */
public class GeoHashUtils {

	private static int PRECISION = 12;
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

	/**
	 * Encodes a coordinate into a geo hash.
	 * @see "http://en.wikipedia.org/wiki/Geohash"
	 * @param latitude
	 * @param longitude
	 * @return geo hash for the coordinate
	 */
	public static String encode(double latitude, double longitude) {
		double[] latInterval = { -90.0, 90.0 };
		double[] lonInterval = { -180.0, 180.0 };

		StringBuilder geohash = new StringBuilder();
		boolean is_even = true;
		int bit = 0, ch = 0;

		while (geohash.length() < PRECISION) {
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
	 * @param geohash
	 * @return a coordinate representing the center of the geohash as a double array of [latitude,longitude]
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
	 * @return double array representing the bounding box for the geohash of [nort latitude, south latitude, east longitude, west longitude]
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
	 * @return the geo hash of the same length directly north of the bounding box.
	 */
	public static String north(String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		double latDiff = bbox[1] - bbox[0];
		double lat = bbox[0] - latDiff / 2;
		double lon = (bbox[2] + bbox[3]) / 2;
		return encode(lat, lon).substring(0, geoHash.length());
	}

	/**
	 * @return the geo hash of the same length directly south of the bounding box.
	 */
	public static String south(String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		double latDiff = bbox[1] - bbox[0];
		double lat = bbox[1] + latDiff / 2;
		double lon = (bbox[2] + bbox[3]) / 2;
		return encode(lat, lon).substring(0, geoHash.length());
	}

	/**
	 * @return the geo hash of the same length directly west of the bounding box.
	 */
	public static String west(String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		double lonDiff = bbox[3] - bbox[2];
		double lat = (bbox[0] + bbox[1]) / 2;
		double lon = bbox[2] - lonDiff / 2;
		return encode(lat, lon).substring(0, geoHash.length());
	}

	/**
	 * @return the geo hash of the same length directly east of the bounding box.
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
		return GeoGeometry.bboxContains(decode_bbox(geoHash), latitude, longitude);
	}

	/**
	 * @param geoHash
	 * @return a BitSet for the given geoHash
	 */
	public static BitSet toBitSet(String geoHash) {
		BitSet bitSet = new BitSet();
		int b=0;
		for (int i = 0; i < geoHash.length(); i++) {
			int currentCharacter = BASE32_DECODE_MAP.get(geoHash.charAt(i));
			for (int z = 0; z < BITS.length; z++) {
				if((currentCharacter & BITS[z]) != 0) {
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
		int ch=0;
		int b=1;
		if(bitSet.length() ==0) {
			return "0";
		}
		for(; b <= bitSet.length(); b++) {
			if(bitSet.get(b-1)) {
				ch += BITS[(b-1)%BITS.length];
			}

			if(b%BITS.length==0 && b!=1) {
				encoded.append(BASE32_CHARS[ch]);
				ch=0;
			}
		}
		// b will have incremented despite failing the check in the for loop; so compensate
		if((b-1)%BITS.length!=0) {
			encoded.append(BASE32_CHARS[ch]);
			ch=0;
		}
		return encoded.toString();
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
	 * the first 4 share the north east 1/8th
	 * the first 8 share the north east 1/4th
	 * the first 16 share the north 1/2
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
	 * Some useful properties:
	 * Anything ending with
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
	public static String[] subHashes(String geoHash) {
		ArrayList<String> list = new ArrayList<>();
		for (char c : BASE32_CHARS) {
			list.add(geoHash+c);
		}
		return list.toArray(new String[0]);
	}
}
