/**
 * Copyright (c) 2012, Jilles van Gurp
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jillesvangurp.geo;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Set;

import static com.jillesvangurp.geo.GeoGeometry.*;
import static com.jillesvangurp.geo.GeoHashUtils.*;
import static java.lang.Math.abs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

@Test
public class GeoHashUtilsTest {

    @DataProvider
    public Object[][] coordinates() {
        return new Object[][]{
                {0.1, -0.1, "ebpbtdpntc6e"}, // very cold there ;-)
                {52.530888, 13.394904, "u33dbfcyegk2"}, // home sweet home
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
        assertThat(encode(lat, lon, DEFAULT_GEO_HASH_LENGTH), is(expectedGeoHash));
        assertThat(encode(new double[]{lon, lat}), is(expectedGeoHash));
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
        double[] bbox = decodeBbox(geoHash);
        assertThat(abs((bbox[0] + bbox[2]) / 2 - lon), lessThan(0.0001));
        assertThat(abs((bbox[1] + bbox[3]) / 2 - lat), lessThan(0.0001));
    }

    @Test(dataProvider = "coordinates")
    public void shouldCalculateEast(Double lat, Double lon, String geoHash) {
        String original = geoHash.substring(0, 3);
        String calculated = GeoHashUtils.east(geoHash.substring(0, 3));
        assertThat("east hash should not contain the coordinate",
                !contains(calculated, lat, lon));
        double[] bbox = decodeBbox(original);
        double[] eastBbox = decodeBbox(calculated);
        assertSimilar(bbox[1], eastBbox[1]);
        assertSimilar(bbox[3], eastBbox[3]);
        assertSimilar(bbox[2], eastBbox[0]);
        assertSimilar((eastBbox[2] - bbox[0]) / 2, bbox[2] - bbox[0]);
        double nl = GeoHashUtils.decode(calculated)[0];
        double ol = GeoHashUtils.decode(original)[0];
        assertThat("decoded hash lon should be east of original", GeoHashUtils.isEast(nl, ol));
    }

    @Test(dataProvider = "coordinates")
    public void shouldCalculateSouth(Double lat, Double lon, String geoHash) {
        String original = geoHash.substring(0, 3);
        String calculatedHash = GeoHashUtils.south(original);
//        System.out.println(original + " " + calculatedHash);
        double[] oBox = decodeBbox(original);
        double[] cBox = decodeBbox(calculatedHash);
        assertThat("calculated hash should not contain the coordinate",
                !contains(calculatedHash, lat, lon));
        double oWest = oBox[0];
        double oSouth = oBox[1];
        double oEast = oBox[2];
        double oNorth = oBox[3];
        double cWest = cBox[0];
        double cSouth = cBox[1];
        double cEast = cBox[2];
        double cNorth = cBox[3];

        assertSimilar((oNorth - cSouth) / 2, oNorth - oSouth);
        assertSimilar(oSouth, cNorth);
        assertSimilar(oEast, cEast);
        assertSimilar(oWest, cWest);
        double nl = GeoHashUtils.decode(calculatedHash)[1];
        double ol = GeoHashUtils.decode(original)[1];
        assertThat("decoded hash lat should be south of original", GeoHashUtils.isSouth(nl, ol));
    }

    @Test(dataProvider = "coordinates")
    public void shouldCalculateNorth(Double lat, Double lon, String geoHash) {
        String original = geoHash.substring(0, 3);
        String calculatedHash = GeoHashUtils.north(original);
        double[] oBox = decodeBbox(original);
        double[] cBox = decodeBbox(calculatedHash);
        assertThat("calculated hash should not contain the coordinate",
                !contains(calculatedHash, lat, lon));
        double oWest = oBox[0];
        double oSouth = oBox[1];
        double oEast = oBox[2];
        double oNorth = oBox[3];
        double cWest = cBox[0];
        double cSouth = cBox[1];
        double cEast = cBox[2];
        double cNorth = cBox[3];
        assertSimilar(oNorth, cSouth);
        assertSimilar((oSouth - cNorth) / 2, oSouth - oNorth);
        assertSimilar(oWest, cWest);
        assertSimilar(oEast, cEast);
//        System.out.println();
        double nl = GeoHashUtils.decode(calculatedHash)[1];
        double ol = GeoHashUtils.decode(original)[1];
        assertThat("decoded hash lat should be north of original", GeoHashUtils.isNorth(nl, ol));
    }

    @Test(dataProvider = "coordinates")
    public void shouldCalculateWest(Double lat, Double lon, String geoHash) {
        String original = geoHash.substring(0, 3);
        String calculatedHash = GeoHashUtils.west(original);
        double[] oBox = decodeBbox(original);
        double[] cBox = decodeBbox(calculatedHash);
        assertThat("calculated hash should not contain the coordinate",
                !contains(calculatedHash, lat, lon));
        double oWest = oBox[0];
        double oSouth = oBox[1];
        double oEast = oBox[2];
        double oNorth = oBox[3];
        double cWest = cBox[0];
        double cSouth = cBox[1];
        double cEast = cBox[2];
        double cNorth = cBox[3];

        assertSimilar(oSouth, cSouth);
        assertSimilar(oNorth, cNorth);
        assertSimilar((oEast - cWest) / 2, oEast - oWest);
        assertSimilar(oWest, cEast);
        double nl = GeoHashUtils.decode(calculatedHash)[0];
        double ol = GeoHashUtils.decode(original)[0];
        assertThat("decoded hash lon should be west of original", GeoHashUtils.isWest(nl, ol));
    }

    private void assertSimilar(double d1, double d2) {
        // allow for some margin of error
        assertThat("should be similar" + d1 + " and " + d2, abs(d1 - d2),
                lessThan(0.0000001));
    }

//    @Test(enabled = false)
//    public void shouldCalculateBboxSizes() {
//        System.out.println("<table border=\"1\">");
//        System.out.println("<th><td>latitude</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>8</td><td>9</td><td>10</td><td>11</td><td>12</td></th>");
//        printHashSizes(90, 0);
//        printHashSizes(80, 0);
//        printHashSizes(70, 0);
//        printHashSizes(60, 0);
//        printHashSizes(50, 0);
//        printHashSizes(40, 0);
//        printHashSizes(30, 0);
//        printHashSizes(20, 0);
//        printHashSizes(10, 0);
//        printHashSizes(0, 0);
//        System.out.println("</table>");
//    }

//    private void printHashSizes(double lat, double lon) {
//        String geoHash = encode(lat, lon, DEFAULT_GEO_HASH_LENGTH);
//
//        // not a test but nice to get a sense of the scale of a geo hash
//        System.out.println("<tr><td>" + lat + "</td>");
//        for (int i = 1; i <= geoHash.length(); i++) {
//            String prefix = geoHash.substring(0, i);
//            double[] bbox = decodeBbox(prefix);
//            double vertical = roundToDecimals(distance(bbox[0], bbox[3], bbox[1], bbox[3]), 2);
//            double horizontal = roundToDecimals(distance(bbox[0], bbox[2], bbox[0], bbox[3]), 2);
//            System.out.print("<td>" + horizontal + "x" + vertical + "</td>");
//        }
//        System.out.print("</tr>\n");
//    }

    @Test(enabled = false)
    public void shouldCalculateSubHashesForHash() {
        String hash = "u33dbfc";
        String[] subHashes = GeoHashUtils.subHashes(hash);
        assertThat(subHashes.length, is(32));
        String first = subHashes[0];
        String row = first;
        for (int j = 0; j < 16; j++) {
            String column = row;
            for (int i = 0; i < 8; i++) {
//                System.out.print(column + " ");
                column = east(column);
            }
//            System.out.println();
            row = north(row);
        }
    }

    @DataProvider
    public Object[][] lines() {
        return new Object[][]{
                {1, 1, 2, 2, "/"},
                {2, 2, 1, 1, "\\"},
                {2, 1, 1, 1, "|"},
                {1, 2, 1, 1, "-"},
                {1, 1, 2, 1, "|"},
                {1, 1, 1, 2, "-"},
                {1, 1, 1, 2, "|"}};
    }

    @Test(dataProvider = "lines")
    public void shouldCalculateHashesForLine(double lat1, double lon1,
                                             double lat2, double lon2, String orientation) {
        Set<String> hashes = GeoHashUtils.geoHashesForLine(10000, lat1, lon1, lat2,
                lon2);

//        GsonBuilder b = new GsonBuilder();
//        Gson gson = b.serializeNulls().create();
//
//
//        PointGeometry p1 = PointGeometry.of(lon1, lat1);
//        PointGeometry p2 = PointGeometry.of(lon2, lat2);
//        double[][] line = p1.line(p2);
//        LineStringGeometry lineGeo = new LineStringGeometry(line,null);
//        System.out.println(gson.toJson(
//                FeatureCollection.of(p1.asFeature(null,null), p2.asFeature(null,null), lineGeo.asFeature(null,null)).plus( FeatureCollection.fromGeoHashes(hashes))));

        assertThat("number of hashes, orientation " + orientation, hashes.size(), greaterThan(10));

    }


    @DataProvider
    public Object[][] samplePoints() {
        return new Double[][]{
                {10.0, 85.0, 15.0},
                {10.0, 50.0, 15.0},
                {10.0, 0.0, 15.0},
                {100.0, 85.0, 15.0},
                {100.0, 50.0, 15.0},
                {100.0, 0.0, 15.0},
                {1000.0, 85.0, 15.0},
                {1000.0, 50.0, 15.0},
                {1000.0, 0.0, 15.0},
                {10000.0, 85.0, 15.0},
                {10000.0, 50.0, 15.0},
                {10000.0, 0.0, 15.0},
                {100000.0, 85.0, 15.0},
                {100000.0, 50.0, 15.0},
                {100000.0, 0.0, 15.0}
        };
    }

    @Test(dataProvider = "samplePoints")
    public void shouldCalculateHashLength(double m, double latitude, double longitude) {
        int length = GeoHashUtils.suitableHashLength(m, latitude, longitude);
        String hash = encode(latitude, longitude, length);
        double[] bbox = decodeBbox(hash);
        double distance = GeoGeometry.distance(bbox[0], bbox[1], bbox[0], bbox[3]);
        assertThat(distance, lessThan(m));
    }
}
