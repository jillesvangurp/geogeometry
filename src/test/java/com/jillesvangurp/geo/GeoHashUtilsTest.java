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
import static com.jillesvangurp.geo.GeoGeometry.roundToDecimals;
import static com.jillesvangurp.geo.GeoHashUtils.contains;
import static com.jillesvangurp.geo.GeoHashUtils.decode;
import static com.jillesvangurp.geo.GeoHashUtils.decode_bbox;
import static com.jillesvangurp.geo.GeoHashUtils.east;
import static com.jillesvangurp.geo.GeoHashUtils.encode;
import static com.jillesvangurp.geo.GeoHashUtils.north;
import static java.lang.Math.abs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
//import static org.hamcrest.number.OrderingComparison.lessThan;

@Test
public class GeoHashUtilsTest {

	@DataProvider
	public Object[][] coordinates() {
		return new Object[][] {
		        { 0.1, -0.1, "ebpbtdpntc6e" }, // very cold there ;-)
				{ 52.530888, 13.394904, "u33dbfcyegk2" }, // home sweet home
		};
	}

	@Test(dataProvider = "coordinates")
	public void shouldDecode(Double lat, Double lon, String geoHash) {
		double[] decoded = decode(geoHash);
		assertSimilar(lat, decoded[1]);
		assertSimilar(lon, decoded[0]);
	}

	@Test(dataProvider = "coordinates")
	public void shouldEncode(Double lat, Double lon, String expectedGeoHash) {
		assertThat(encode(lat, lon), is(expectedGeoHash));
        assertThat(encode(new double[]{lon,lat}), is(expectedGeoHash));
	}

	@Test(dataProvider = "coordinates")
	public void shouldContainCoordinate(Double lat, Double lon, String geoHash) {
		assertThat("hash should contain the coordinate",
				contains(geoHash, lat, lon));
		assertThat("hash should not contain the swapped coordinate",
				!contains(geoHash, lon, lat));
	}

	@Test(dataProvider = "coordinates")
	public void shouldDecodeBbox(Double lat, Double lon, String geoHash) {
		double[] bbox = decode_bbox(geoHash);
		assertThat(abs((bbox[0] + bbox[1]) / 2 - lat), lessThan(0.0001));
		assertThat(abs((bbox[2] + bbox[3]) / 2 - lon), lessThan(0.0001));
	}

	@Test(dataProvider = "coordinates")
	public void shouldCalculateEast(Double lat, Double lon, String geoHash) {
		String original = geoHash.substring(0, 3);
		String calculated = GeoHashUtils.east(geoHash.substring(0, 3));
		assertThat("east hash should not contain the coordinate",
		        !contains(calculated, lat, lon));
		double[] bbox = decode_bbox(original);
		double[] eastBbox = decode_bbox(calculated);
		assertSimilar(bbox[0], eastBbox[0]);
		assertSimilar(bbox[1], eastBbox[1]);
		assertSimilar(bbox[3], eastBbox[2]);
		assertSimilar((eastBbox[3] - bbox[2]) / 2, bbox[3] - bbox[2]);
        double nl = GeoHashUtils.decode(calculated)[0];
        double ol = GeoHashUtils.decode(original)[0];
        assertThat("decoded hash lon should be east of original",GeoHashUtils.isEast(nl, ol));
	}

	public void shouldCalculateEastOn180() {
	    String hash = encode(-18, 179.9,3);
	    double[] bbox = decode_bbox(hash);
	    assertThat(bbox[3], equalTo(180.0));
	    String east = GeoHashUtils.east(hash);
	    bbox = decode_bbox(east);
        assertThat(bbox[2], equalTo(-180.0));
	}

    public void shouldCalculateWestOn180() {
        String hash = encode(-18, -179.9, 3);
        double[] bbox = decode_bbox(hash);
        assertThat(bbox[2], equalTo(-180.0));
        String west = GeoHashUtils.west(hash);
        bbox = decode_bbox(west);
        assertThat(bbox[3], equalTo(180.0));
    }

	@Test(dataProvider = "coordinates")
	public void shouldCalculateSouth(Double lat, Double lon, String geoHash) {
		String original = geoHash.substring(0, 3);
		String calculated = GeoHashUtils.south(original);
		assertThat("north hash should not contain the coordinate",
				!contains(calculated, lat, lon));
		double[] bbox = decode_bbox(geoHash.substring(0, 3));
		double[] northBbox = decode_bbox(calculated);
		assertSimilar((bbox[1] - northBbox[0]) / 2, bbox[1] - bbox[0]);
		assertSimilar(bbox[0], northBbox[1]);
		assertSimilar(bbox[2], northBbox[2]);
		assertSimilar(bbox[3], northBbox[3]);
        double nl = GeoHashUtils.decode(calculated)[1];
        double ol = GeoHashUtils.decode(original)[1];
        assertThat("decoded hash lat should be south of original",GeoHashUtils.isSouth(nl, ol));
	}

	@Test(dataProvider = "coordinates")
	public void shouldCalculateNorth(Double lat, Double lon, String geoHash) {
		String original = geoHash.substring(0, 3);
		String calculatedHash = GeoHashUtils.north(original);
		double[] bbox = decode_bbox(geoHash.substring(0, 3));
		double[] southBbox = decode_bbox(calculatedHash);
		assertThat("calculated hash should not contain the coordinate",
				!contains(calculatedHash, lat, lon));
		assertSimilar(bbox[1], southBbox[0]);
		assertSimilar((bbox[0] - southBbox[1]) / 2, bbox[0] - bbox[1]);
		assertSimilar(bbox[2], southBbox[2]);
		assertSimilar(bbox[3], southBbox[3]);		System.out.println();
		double nl = GeoHashUtils.decode(calculatedHash)[1];
        double ol = GeoHashUtils.decode(original)[1];
        assertThat("decoded hash lat should be north of original",GeoHashUtils.isNorth(nl, ol));
	}

	@Test(dataProvider = "coordinates")
	public void shouldCalculateWest(Double lat, Double lon, String geoHash) {
		String original = geoHash.substring(0, 3);
		String calculated = GeoHashUtils.west(original);
		assertThat("west hash should not contain the coordinate",
				!contains(calculated, lat, lon));
		double[] bbox = decode_bbox(geoHash.substring(0, 3));
		double[] westBbox = decode_bbox(calculated);
		assertSimilar(bbox[0], westBbox[0]);
		assertSimilar(bbox[1], westBbox[1]);
		assertSimilar((bbox[3] - westBbox[2]) / 2, bbox[3] - bbox[2]);
		assertSimilar(bbox[2], westBbox[3]);
        double nl = GeoHashUtils.decode(calculated)[0];
        double ol = GeoHashUtils.decode(original)[0];
        assertThat("decoded hash lon should be west of original",GeoHashUtils.isWest(nl, ol));
	}

	private void assertSimilar(double d1, double d2) {
		// allow for some margin of error
		assertThat("should be similar" + d1 + " and " + d2, abs(d1 - d2),
				lessThan(0.0000001));
	}

	@Test(enabled=false)
	public void shouldCalculateBboxSizes() {
	    System.out.println("<table border=\"1\">");
	    System.out.println("<th><td>latitude</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>8</td><td>9</td><td>10</td><td>11</td><td>12</td></th>");
        printHashSizes(90, 0);
        printHashSizes(80, 0);
        printHashSizes(70, 0);
        printHashSizes(60, 0);
        printHashSizes(50, 0);
        printHashSizes(40, 0);
        printHashSizes(30, 0);
        printHashSizes(20, 0);
        printHashSizes(10, 0);
        printHashSizes(0, 0);
        System.out.println("</table>");
	}

    private void printHashSizes(double lat, double lon) {
        String geoHash=encode(lat, lon);

		// not a test but nice to get a sense of the scale of a geo hash
        System.out.println("<tr><td>"+lat+"</td>");
		for (int i = 1; i <= geoHash.length(); i++) {
			String prefix = geoHash.substring(0, i);
			double[] bbox = decode_bbox(prefix);
			double vertical = roundToDecimals(distance(bbox[0], bbox[3], bbox[1], bbox[3]),2);
			double horizontal = roundToDecimals(distance(bbox[0], bbox[2], bbox[0], bbox[3]),2);
			System.out.print("<td>"+horizontal+"x"+vertical+"</td>");
		}
		System.out.print("</tr>\n");
    }

	@Test(enabled=false)
	public void shouldCalculateSubHashesForHash() {
		String hash = "u33dbfc";
		String[] subHashes = GeoHashUtils.subHashes(hash);
		assertThat(subHashes.length, is(32));
		String first = subHashes[0];
		String row = first;
		for (int j = 0; j < 16; j++) {
			String column = row;
			for (int i = 0; i < 8; i++) {
				System.out.print(column + " ");
				column = east(column);
			}
			System.out.println();
			row = north(row);
		}
	}

	double[][] polygon = new double[][] { { -1, 1 }, { 2, 2 }, { 3, -1 },
			{ -2, -4 } };

	public void shouldCalculateHashesForPolygon() {
		int min = 10;
		Set<String> geoHashesForPolygon = GeoHashUtils.geoHashesForPolygon(
				8, polygon);
		for (String h : geoHashesForPolygon) {
			min = Math.min(min, h.length());
		}
		assertThat("there should be some hashes with length=3", min, is(4));
		assertThat("huge area, should generate lots of hashes",
				geoHashesForPolygon.size() > 1000);
	}

	public void shouldCalculateHashesForCircle() {
	    Set<String> hashesForCircle = GeoHashUtils.geoHashesForCircle(8, 52, 13, 2000);
	    for (String hash : hashesForCircle) {
            double[] point = GeoHashUtils.decode(hash);
            double distance = GeoGeometry.distance(point, new double[]{13,52});
            assertThat(distance, lessThan(2000.0));
        }
	}

	@DataProvider
	public Object[][] lines() {
		return new Object[][] { { 1, 1, 2, 2 }, { 2, 2, 1, 1 }, { 2, 1, 1, 1 },
				{ 1, 2, 1, 1 }, { 1, 1, 2, 1 }, { 1, 1, 1, 2 },{1,1,1,2} };
	}

	@Test(dataProvider = "lines")
	public void shouldCalculateHashesForLine(double lat1, double lon1,
			double lat2, double lon2) {
		Set<String> hashes = GeoHashUtils.geoHashesForLine(1000, lat1, lon1, lat2,
				lon2);

		assertThat(hashes.size(), greaterThan(10));

	}

	public void shouldCheckIfWest() {
	    assertThat("should be west", GeoHashUtils.isWest(90, 91));
	    assertThat("should be west", GeoHashUtils.isWest(-1, 1));
        assertThat("should be west", GeoHashUtils.isWest(-89, 90));
        assertThat("should be west", GeoHashUtils.isWest(180,-178));
        assertThat("should be west", GeoHashUtils.isWest(180,-179.99527198651967));
        assertThat("should not be west", !GeoHashUtils.isWest(-179, 180));
        assertThat("should not be west", !GeoHashUtils.isWest(91, 90));
        assertThat("should not be west", !GeoHashUtils.isWest(-179, 180));
        assertThat("should not be west", !GeoHashUtils.isWest(89, -90));
        assertThat("should not be west", !GeoHashUtils.isWest(1, -1));
        assertThat("should not be west", !GeoHashUtils.isWest(91, 90));
        assertThat("should not be west", !GeoHashUtils.isWest(-91, 90));
	}

    public void shouldCheckIfEast() {
        assertThat("should not be east", !GeoHashUtils.isEast(90, 91));
        assertThat("should not be east", !GeoHashUtils.isEast(-1, 1));
        assertThat("should not be east", !GeoHashUtils.isEast(-89, 90));
        assertThat("should not be east", !GeoHashUtils.isEast(180, -178));
        assertThat("should not be west", !GeoHashUtils.isEast(180, -179.99527198651967));
        assertThat("should be east", GeoHashUtils.isEast(-179, 180));
        assertThat("should be east", GeoHashUtils.isEast(91, 90));
        assertThat("should be east", GeoHashUtils.isEast(-179, 180));
        assertThat("should be east", GeoHashUtils.isEast(89, -90));
        assertThat("should be east", GeoHashUtils.isEast(1, -1));
        assertThat("should be east", GeoHashUtils.isEast(91, 90));
        assertThat("should be east", GeoHashUtils.isEast(-91, 90));
    }

	public void shouldBeNeitherWestNorEast() {
        assertThat("should not be west", !GeoHashUtils.isWest(-90, 90));
        assertThat("should not be east", !GeoHashUtils.isEast(-90, 90));
	}

    @DataProvider
    public Double[][] samplePoints() {
        return new Double[][] {
                { 10.0, 85.0, 15.0 },
                { 10.0, 50.0, 15.0 },
                { 10.0, 0.0, 15.0 },
                { 100.0, 85.0, 15.0 },
                { 100.0, 50.0, 15.0 },
                { 100.0, 0.0, 15.0 },
                { 1000.0, 85.0, 15.0 },
                { 1000.0, 50.0, 15.0 },
                { 1000.0, 0.0, 15.0 },
                { 10000.0, 85.0, 15.0 },
                { 10000.0, 50.0, 15.0 },
                { 10000.0, 0.0, 15.0 },
                { 100000.0, 85.0, 15.0 },
                { 100000.0, 50.0, 15.0 },
                { 100000.0, 0.0, 15.0 }
        };
    }

    @Test(dataProvider="samplePoints")
	public void shouldCalculateHashLength(double m, double latitude, double longitude) {
        int length = GeoHashUtils.suitableHashLength(m, latitude, longitude);
        String hash = encode(latitude, longitude, length);
        double[] bbox = decode_bbox(hash);
        double distance = GeoGeometry.distance(bbox[0], bbox[2], bbox[0], bbox[3]);
        assertThat(distance, lessThan(m));
	}

    public void shouldGenerateCircleHashesThatAreAllWithinRadiusOfCircle() {
        double latitude = 52.529731;
        double longitude = 13.401284;
        int radius = 500;
        Set<String> hashes = GeoHashUtils.geoHashesForCircle(8, latitude,longitude, radius);
        for (String hash : hashes) {
            assertThat(distance(decode(hash), new double[] {longitude,latitude}), lessThan(500.0));
        }
        assertThat(hashes.size(), greaterThan(radius));
    }
}
