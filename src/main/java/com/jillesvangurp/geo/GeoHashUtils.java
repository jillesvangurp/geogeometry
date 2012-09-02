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
	private static char[] BASE32_CHARS = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm',
			'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	private final static Map<Character, Integer> BASE32_DECODE_MAP = new HashMap<Character, Integer>();
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
		double[] lat_interval = { -90.0, 90.0 };
		double[] lon_interval = { -180.0, 180.0 };

		StringBuilder geohash = new StringBuilder();
		boolean is_even = true;
		int bit = 0, ch = 0;

		while (geohash.length() < PRECISION) {
			double mid = 0.0;
			if (is_even) {
				mid = (lon_interval[0] + lon_interval[1]) / 2;
				if (longitude > mid) {
					ch |= BITS[bit];
					lon_interval[0] = mid;
				} else {
					lon_interval[1] = mid;
				}

			} else {
				mid = (lat_interval[0] + lat_interval[1]) / 2;
				if (latitude > mid) {
					ch |= BITS[bit];
					lat_interval[0] = mid;
				} else {
					lat_interval[1] = mid;
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
	 * @return a coordinate as a double array of [latitude,longitude]
	 */
	public static double[] decode(String geohash) {
		double[] ge = decode_exactly(geohash);
		double lat, lon, lat_err, lon_err;
		lat = ge[0];
		lon = ge[1];
		lat_err = ge[2];
		lon_err = ge[3];

		double lat_precision = Math.max(1, Math.round(-Math.log10(lat_err))) - 1;
		double lon_precision = Math.max(1, Math.round(-Math.log10(lon_err))) - 1;

		lat = getPrecision(lat, lat_precision);
		lon = getPrecision(lon, lon_precision);

		return new double[] { lat, lon };
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

			int cd = BASE32_DECODE_MAP.get(geohash.charAt(i));

			for (int z = 0; z < BITS.length; z++) {
				int mask = BITS[z];
				if (is_even) {
					if ((cd & mask) != 0) {
						lon_interval[0] = (lon_interval[0] + lon_interval[1]) / 2;
					} else {
						lon_interval[1] = (lon_interval[0] + lon_interval[1]) / 2;
					}

				} else {

					if ((cd & mask) != 0) {
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
		double[] bbox = decode_bbox(geoHash);
		return latitude >= bbox[0] && latitude <= bbox[1]
				&& longitude >= bbox[2] && longitude <= bbox[3];
	}

	private static double[] decode_exactly(String geohash) {
		double[] lat_interval = { -90.0, 90.0 };
		double[] lon_interval = { -180.0, 180.0 };

		double lat_err = 90.0;
		double lon_err = 180.0;
		boolean is_even = true;
		double latitude, longitude;
		for (int i = 0; i < geohash.length(); i++) {

			int cd = BASE32_DECODE_MAP.get(geohash.charAt(i));

			for (int z = 0; z < BITS.length; z++) {
				int mask = BITS[z];
				if (is_even) {
					lon_err /= 2;
					if ((cd & mask) != 0) {
						lon_interval[0] = (lon_interval[0] + lon_interval[1]) / 2;
					} else {
						lon_interval[1] = (lon_interval[0] + lon_interval[1]) / 2;
					}

				} else {
					lat_err /= 2;

					if ((cd & mask) != 0) {
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

		return new double[] { latitude, longitude, lat_err, lon_err };
	}

	private static double getPrecision(double x, double precision) {
		double base = Math.pow(10, -precision);
		double diff = x % base;
		return x - diff;
	}
}
