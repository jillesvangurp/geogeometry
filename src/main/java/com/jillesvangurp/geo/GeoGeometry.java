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
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class GeoGeometry {

	/**
	 * @param polygon a 2D double array of [[latitude,longitude]]
	 * @return bounding box that contains the polygon as a double array of [minLat,maxLat,minLon,maxLon}
	 */
	public static double[] getBbox(double[][] polygon) {
		double minLat=Integer.MAX_VALUE;
		double minLon=Integer.MAX_VALUE;
		double maxLat=Integer.MIN_VALUE;
		double maxLon=Integer.MIN_VALUE;

		for(int i=0; i<polygon.length;i++) {
			minLat = min(minLat, polygon[i][0]);
			minLon = min(minLon, polygon[i][1]);
			maxLat = max(maxLat, polygon[i][0]);
			maxLon = max(maxLon, polygon[i][1]);
		}

		return new double[] {minLat,maxLat,minLon,maxLon};
	}

	/**
	 * @param bbox double array of [minLat,maxLat,minLon,maxLon}
	 * @param latitude
	 * @param longitude
	 * @return true if the latitude and longitude are contained in the bbox
	 */
	public static boolean bboxContains(double[] bbox, double latitude,double longitude) {
		return bbox[0] <= latitude && latitude <= bbox[1] && bbox[2] <= longitude && longitude <= bbox[3];
	}

	/**
	 * @param polygon a 2D double array of [[latitude,longitude]]
	 * @param latitude
	 * @param longitude
	 * @return true if the polygon contains the coordinate
	 */
	public static boolean polygonContains(double[][] polygon, double latitude, double longitude) {

		if (polygon.length <= 2) {
			// that would be a line
			return false;
		}

		double[] bbox = getBbox(polygon);
		if(!bboxContains(bbox, latitude, longitude)) {
			// outside the containing bbox
			return false;
		}

		int hits = 0;

		double lastLatitude = polygon[polygon.length - 1][0];
		double lastLongitude = polygon[polygon.length - 1][1];
		double currentLatitude, currentLongitude;

		// Walk the edges of the polygon
		for (int i = 0; i < polygon.length; lastLatitude = currentLatitude, lastLongitude = currentLongitude, i++) {
			currentLatitude = polygon[i][0];
			currentLongitude = polygon[i][1];

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
