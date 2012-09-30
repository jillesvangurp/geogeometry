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

import static com.jillesvangurp.geo.GeoGeometry.bboxContains;
import static com.jillesvangurp.geo.GeoGeometry.distance;
import static com.jillesvangurp.geo.GeoGeometry.polygonContains;
import static com.jillesvangurp.geo.GeoGeometry.roundToDecimals;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class GeoGeometryTest {

    double[] sydney=new double[] {-33.872796,151.206146};
    double[] buenosaires=new double[] {-34.602875,-58.380449};
    double[] newyork=new double[] {40.721119,-74.011237};
    double[] amsterdam=new double[] {52.372103,4.894252};
    double[] berlin=new double[] {52.527109,13.385721};
    double[] london=new double[] {51.51283,-0.123656};


    double[] brandenBurgerGate=new double[] {52.516279,13.377157};
    double[] potsDammerPlatz=new double[] {52.509515,13.376599};
    double[] moritzPlatz=new double[] {52.503663,13.410717};
    double[] senefelderPlatz=new double[] {52.532755,13.412949};
    double[] naturkundeMuseum=new double[] {52.531188,13.381921};
    double[] rosenthalerPlatz=new double[] {52.529948,13.401361};
    double[] oranienburgerTor=new double[] {52.525339,13.38707};


	double[][] samplePolygon = new double[][] {
		{ -1, 1 },
		{ 2, 2 },
		{ 3, -1 },
		{ -2, -4 }
	};


	public void shouldCheckThatLinesCross() {
	    assertThat("should intersect", GeoGeometry.linesCross(1, 1, 2, 2, 1, 2, 2, 1));
        assertThat("should intersect (vertical)", GeoGeometry.linesCross(1, 1, 1, 10, 1, 3, 1, 4));
        assertThat("should intersect (horizontal)", GeoGeometry.linesCross(1, 666, 10, 666, 3, 666, 4, 666));
	}

	public void shouldCheckThatLinesDontCross() {
        assertThat("should not intersect lines intersect but not in the specified interval", !GeoGeometry.linesCross(1, 2, 3, 4, 10, 20, 20, 10));
	    assertThat("should not intersect parallel", !GeoGeometry.linesCross(1, 1, 2, 2, 2, 2, 3, 3));
        assertThat("should not intersect same vertical line but not overlapping", !GeoGeometry.linesCross(1, 1, 1, 5, 1, 6, 1, 10));
        assertThat("should not intersect same horizontal line but not overlapping", !GeoGeometry.linesCross(1, 666, 5, 666, 6, 666, 10, 666));
	}

	public void shouldCheckContainmentForPolygonCorrectly() {
		assertThat("origin should be in there", polygonContains(0, 0, samplePolygon));
		assertThat("should be outside", !polygonContains(20, 20, samplePolygon));
		assertThat("should be inside", polygonContains(0.5, 0.5, samplePolygon));
		assertThat("should be inside", polygonContains(0.5, -0.5, samplePolygon));
		assertThat("should be inside", polygonContains(-0.5, 0.5, samplePolygon));
		assertThat("should be inside", polygonContains(-0.5, -0.5, samplePolygon));
	}

	public void shouldBboxContainPoint() {
		double bbox[] = new double[] {-1,1,-2,2};

		assertThat("should be contained", bboxContains(bbox, 0, 0));
		assertThat("should be contained", bboxContains(bbox, 1, 2));
		assertThat("should be contained", bboxContains(bbox, -1, 2));
		assertThat("should be contained", bboxContains(bbox, 1, 2));
		assertThat("should be contained", bboxContains(bbox, -1, -2));
		assertThat("should not be contained", !bboxContains(bbox, -1.1, -2.1));
	}

	public void shouldGetCorrectBboxForPolygon() {
		double[] bbox = GeoGeometry.getBbox(samplePolygon);
		assertThat("should be contained", bboxContains(bbox, 0, 0));
		for (double[] coordinate : samplePolygon) {
			assertThat("should contain point", bboxContains(bbox, coordinate[0], coordinate[1]));
			assertThat("should not contain point", !bboxContains(bbox, 20*coordinate[0], 20*coordinate[1]));
		}
	}

	public void shouldRoundDouble() {
		assertThat(roundToDecimals(0.1234567891111112, 17), is(0.1234567891111112));
	}

	public void shouldCalculateDistance() {
		double d  = distance(sydney, berlin);
		assertThat("should be about 16100km but was " + d/1000 + "km.", abs(d - 16000000) < 100000);
	}

	public void shouldTranslateCorrectly() {
	    double[] translated = GeoGeometry.translate(52.530564,13.394964, 1000, 3000);
	    double pythagorasDistance = Math.sqrt(pow(1000, 2)+pow(3000,2));
	    double distance = distance(new double[]{52.530564,13.394964}, translated);
        assertThat("distance should be correct for translated coordinate", abs(distance - pythagorasDistance) < 1.0);
	}

	public void shouldHaveDistanceOfRadiusForEachPoint() {
	    int radius = 50000;
        int segments = 500;
        double[][] polygon = GeoGeometry.circle2polygon(segments, london[0],london[1], radius);
	    double d=0;
	    double[] last = null;

	    for(double[] point: polygon) {
	        if(last != null) {
	            d+=distance(last, point);
	        }
	        double distance = distance(new double[]{london[0],london[1]}, point);
            double difference = abs(radius-distance);
            assertThat("should have distance of radius to origin within 100 meter but was " + difference, difference < 100);
            last=point;
	    }
	    // close the circle
        d+=distance(polygon[0],last);
        double difference = abs(d-2*Math.PI*radius);
        assertThat("circumference should be within 100 meter of length of the polygon but difference was " + difference, difference < 100);
	}

	public void polygonForPointsShouldContainStuffInside() {
	    double[][] placesInMitte = new double[][] {
	            brandenBurgerGate,potsDammerPlatz,moritzPlatz,senefelderPlatz,naturkundeMuseum
	    };
	    double[][] polygon = GeoGeometry.pointCloudToPolygon(placesInMitte);

        assertThat("should be inside", polygonContains(rosenthalerPlatz, polygon));
        assertThat("should be inside", polygonContains(oranienburgerTor, polygon));
        assertThat("should NOT be inside", !polygonContains(1,1, polygon));
	}

	public void polygonForPointsInFourQuadrantsShouldContainStuffInside() {
	    double[][] polygon = GeoGeometry.pointCloudToPolygon(new double[][]{sydney,newyork,amsterdam,buenosaires});
        assertThat("should be inside", polygonContains(london, polygon));
        assertThat("should NOT be inside", !polygonContains(berlin, polygon));
	}

   public void shouldConvertCircleToPolygonOn180() {
        double[][] circle2polygon = GeoGeometry.circle2polygon(6, -18, 180, 1000);
        int countEast=0;
        for (double[] point : circle2polygon) {
            double distance = distance(-18, 180,point[0],point[1]);
            assertThat(abs(1000-distance), lessThan(1.0));
            if(GeoHashUtils.isWest(180, point[1])) {
                countEast++;
            }
        }
        assertThat(countEast, greaterThan(1));
    }

   public void shouldConvertCircleToPolygonOnNorthPole() {

       double lat=89.9;
       double lon=0;
        double[][] circle2polygon = GeoGeometry.circle2polygon(6, lat, lon, 1000);
        int countEast=0;
        for (double[] point : circle2polygon) {
            double distance = distance(lat, lon,point[0],point[1]);
            assertThat(abs(1000-distance), lessThan(20.0));
            if(GeoHashUtils.isWest(180, point[1])) {
                countEast++;
            }
        }
        assertThat(countEast, greaterThan(1));
    }

   @DataProvider
   public Object[][] degrees() {
       return new Object[][] {
               {"W",111,38,45.40, 111.64594444444445},
               {"E",111,38,45.40, -111.64594444444445}
       };

   }

    @Test(dataProvider = "degrees")
    public void shouldConvertToDecimalDegree(String direction, double degrees, double minutes, double seconds, double expected) {
        double decimalDegree = GeoGeometry.toDecimalDegree(direction, degrees, minutes, seconds);
        assertThat(decimalDegree, is(expected));

    }
}
