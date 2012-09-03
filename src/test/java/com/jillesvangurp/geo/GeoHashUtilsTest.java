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

import static com.jillesvangurp.geo.GeoGeometry.distance;
import static com.jillesvangurp.geo.GeoHashUtils.contains;
import static com.jillesvangurp.geo.GeoHashUtils.decode;
import static com.jillesvangurp.geo.GeoHashUtils.decode_bbox;
import static com.jillesvangurp.geo.GeoHashUtils.east;
import static com.jillesvangurp.geo.GeoHashUtils.encode;
import static com.jillesvangurp.geo.GeoHashUtils.fromBitSet;
import static com.jillesvangurp.geo.GeoHashUtils.south;
import static com.jillesvangurp.geo.GeoHashUtils.toBitSet;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThan;

import java.util.BitSet;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class GeoHashUtilsTest {

	@DataProvider
	public Object[][] coordinates() {
		return new Object[][] {
			{ 0.1, -0.1, "ebpbtdpntc6e" }, // very cold there ;-)
			{ 52.530888,13.394904, "u33dbfcyegk2" } // home sweet home
		};
	}

	@Test(dataProvider="coordinates")
	public void shouldDecode(Double lat, Double lon, String geoHash) {
		double[] decoded = decode(geoHash);
		assertSimilar(lat,decoded[0]);
		assertSimilar(lon,decoded[1]);
	}

	@Test(dataProvider="coordinates")
	public void shouldEncode(Double lat, Double lon, String expectedGeoHash) {
		String geoHash = encode(lat, lon);
		assertThat(geoHash, is(expectedGeoHash));
	}

	@Test(dataProvider="coordinates")
	public void shouldContainCoordinate(Double lat, Double lon, String geoHash) {
		assertThat("hash should contain the coordinate", contains(geoHash, lat, lon));
		assertThat("hash should not contain the swapped coordinate", !contains(geoHash, lon, lat));
	}


	@Test(dataProvider="coordinates")
	public void shouldDecodeBbox(Double lat, Double lon, String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		assertThat(abs((bbox[0]+bbox[1])/2-lat), lessThan(0.0001));
		assertThat(abs((bbox[2]+bbox[3])/2-lon), lessThan(0.0001));
	}

	@Test(dataProvider="coordinates")
	public void shouldCalculateEast(Double lat, Double lon, String geoHash) {
		String east = GeoHashUtils.east(geoHash.substring(0,3));
		assertThat("east hash should not contain the coordinate", !contains(east, lat, lon));
		String prefix = geoHash.substring(0,3);
		double[] bbox = decode_bbox(prefix);
		double[] eastBbox = decode_bbox(east);
		assertSimilar(bbox[0], eastBbox[0]);
		assertSimilar(bbox[1], eastBbox[1]);
		assertSimilar(bbox[3], eastBbox[2]);
		assertSimilar((eastBbox[3]-bbox[2])/2, bbox[3]-bbox[2]);
	}

	@Test(dataProvider="coordinates")
	public void shouldCalculateNorth(Double lat, Double lon, String geoHash) {
		String prefix = geoHash.substring(0,3);
		String north = GeoHashUtils.north(prefix);
		assertThat("north hash should not contain the coordinate", !contains(north, lat, lon));
		double[] bbox = decode_bbox(geoHash.substring(0,3));
		double[] northBbox = decode_bbox(north);
		assertSimilar((bbox[1]-northBbox[0])/2, bbox[1]-bbox[0]);
		assertSimilar(bbox[0], northBbox[1]);
		assertSimilar(bbox[2], northBbox[2]);
		assertSimilar(bbox[3], northBbox[3]);
	}

	@Test(dataProvider="coordinates")
	public void shouldCalculateSouth(Double lat, Double lon, String geoHash) {
		String prefix = geoHash.substring(0,3);
		String south = GeoHashUtils.south(prefix);
		double[] bbox = decode_bbox(geoHash.substring(0,3));
		double[] southBbox = decode_bbox(south);
		assertThat("south hash should not contain the coordinate", !contains(south, lat, lon));
		assertSimilar(bbox[1], southBbox[0]);
		assertSimilar((bbox[0]-southBbox[1])/2, bbox[0]-bbox[1]);
		assertSimilar(bbox[2], southBbox[2]);
		assertSimilar(bbox[3], southBbox[3]);
	}

	@Test(dataProvider="coordinates")
	public void shouldCalculateWest(Double lat, Double lon, String geoHash) {
		String prefix = geoHash.substring(0,3);
		String west = GeoHashUtils.west(prefix);
		assertThat("west hash should not contain the coordinate", !contains(west, lat, lon));
		double[] bbox = decode_bbox(geoHash.substring(0,3));
		double[] westBbox = decode_bbox(west);
		assertSimilar(bbox[0], westBbox[0]);
		assertSimilar(bbox[1], westBbox[1]);
		assertSimilar((bbox[3]-westBbox[2])/2, bbox[3]-bbox[2]);
		assertSimilar(bbox[2], westBbox[3]);
	}

	private void assertSimilar(double d1, double d2) {
		// allow for some margin of error
		assertThat("should be similar" + d1 + " and " + d2, abs(d1-d2), lessThan(0.0000001));
	}

	@Test(dataProvider="coordinates")
	public void shouldCalculateBboxSizes(Double lat, Double lon, String geoHash) {
		// not a test but nice to get a sense of the scale of a geo hash
		for(int i=1;i<geoHash.length();i++) {
			String prefix = geoHash.substring(0,i);
			double[] bbox = decode_bbox(prefix);
			long vertical = round(distance(bbox[0],bbox[3], bbox[1], bbox[3]));
			long horizontal = round(distance(bbox[0],bbox[2], bbox[0], bbox[3]));
			System.out.println(i + " " + prefix + ": " + horizontal + " x " + vertical);
		}
	}

	public void shouldConvertToAndFromBitset() {
		String hash = "u33dbfcyegk2";
		for(int i=0;i<hash.length()-2; i++) {
			String prefix = hash.substring(0, hash.length()-i);
			BitSet bitSet = toBitSet(prefix);
			assertThat(bitSet.length(), lessThan(5*prefix.length()+1));
			assertThat(fromBitSet(bitSet), is(prefix));
		}
	}

	public void shouldCalculateSubHashesForHash() {
		String hash = "u33dbfc";
		String[] subHashes = GeoHashUtils.subHashes(hash);
		assertThat(subHashes.length, is(32));
		String first = subHashes[0];
		String row=first;
		for(int j=0; j< 16; j++) {
			String column=row;
			for(int i=0; i<8 ;i++) {
				System.out.print(column + " ");
				column=east(column);
			}
			System.out.println();
			row = south(row);
		}
	}

	double[][] polygon = new double[][] {
			{ -1, 1 },
			{ 2, 2 },
			{ 3, -1 },
			{ -2, -4 }
		};

	public void shouldCalculateHashesForPolygon() {
		int min=10;
		Set<String> geoHashesForPolygon = GeoHashUtils.getGeoHashesForPolygon(8,polygon);
		for(String h: geoHashesForPolygon) {
			min=Math.min(min,h.length());
		}
		System.out.println(geoHashesForPolygon.size());
		assertThat("there should be some hashes with length=3",min, is(3));
		assertThat("huge area, should generate lots of hashes", geoHashesForPolygon.size() > 1000);
	}
}
