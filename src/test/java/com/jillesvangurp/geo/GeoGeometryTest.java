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

import static com.jillesvangurp.geo.GeoGeometry.area;
import static com.jillesvangurp.geo.GeoGeometry.bboxContains;
import static com.jillesvangurp.geo.GeoGeometry.boundingBox;
import static com.jillesvangurp.geo.GeoGeometry.circle2polygon;
import static com.jillesvangurp.geo.GeoGeometry.distance;
import static com.jillesvangurp.geo.GeoGeometry.polygonContains;
import static com.jillesvangurp.geo.GeoGeometry.roundToDecimals;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class GeoGeometryTest {

    double[] sydney=new double[] {151.206146,-33.872796};
    double[] buenosaires=new double[] {-58.380449,-34.602875};
    double[] newyork=new double[] {-74.011237,40.721119};
    double[] amsterdam=new double[] {4.894252,52.372103};
    double[] berlin=new double[] {13.385721,52.527109};
    double[] london=new double[] {-0.123656,51.51283};


    double[] brandenBurgerGate=new double[] {13.377157,52.516279};
    double[] potsDammerPlatz=new double[] {13.376599,52.509515};
    double[] moritzPlatz=new double[] {13.410717,52.503663};
    double[] senefelderPlatz=new double[] {13.412949,52.532755};
    double[] naturkundeMuseum=new double[] {13.381921,52.531188};
    double[] rosenthalerPlatz=new double[] {13.401361,52.529948};
    double[] oranienburgerTor=new double[] {13.38707,52.525339};


	double[][] samplePolygon = new double[][] {
	        {1,1},
	        {1,-1},
	        {-1,-1},
	        {-1,1}
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
		double[] bbox = GeoGeometry.boundingBox(samplePolygon);
		assertThat("should be contained", bboxContains(bbox, 0, 0));
		for (double[] coordinate : samplePolygon) {
			assertThat("should contain point", bboxContains(bbox, coordinate[1], coordinate[0]));
			assertThat("should not contain point", !bboxContains(bbox, 20*coordinate[1], 20*coordinate[0]));
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
	    double distance = distance(new double[]{13.394964,52.530564}, translated);
        assertThat("distance should be correct for translated coordinate", abs(distance - pythagorasDistance) < 1.0);
	}

	public void shouldHaveDistanceOfRadiusForEachPoint() {
	    int radius = 50000;
        int segments = 500;
        double[][] polygon = GeoGeometry.circle2polygon(segments, london[1],london[0], radius);
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
	    double[][] polygon = GeoGeometry.polygonForPoints(placesInMitte);

        assertThat("should be inside", polygonContains(rosenthalerPlatz, polygon));
        assertThat("should be inside", polygonContains(oranienburgerTor, polygon));
        assertThat("should NOT be inside", !polygonContains(1,1, polygon));
	}

	public void shouldCreateSensiblePolygon() {
	    /*
	     * [[[52.49768,13.31191],[52.499,13.313],[52.5007,13.3065],[52.4957,13.3033],[52.49939,13.30523],[52.5004,13.312],[52.50011,13.30723],[52.49907,13.31225],[52.50019,13.30983],[52.49984,13.30646],[52.4968,13.3053],[52.5002,13.3103],[52.50083333333333,13.312777777777779],[52.5,13.307222222222222]],
	     * [[52.49878,13.33396],[52.49604,13.31996],[52.50045,13.31918],[52.49632,13.32024],[52.49983,13.34196],[52.503,13.3316],[52.50277,13.32743],[52.5005,13.3288],[52.50272,13.32918],[52.5029,13.3275],[52.5025,13.3291],[52.5027,13.332],[52.507,13.347],[52.5063,13.3445],[52.5044,13.3428],[52.505,13.339],[52.505,13.335],[52.50444,13.33889],[52.5017,13.3411],[52.50394,13.34048],[52.50624,13.34407],[52.506,13.33193],[52.50456,13.33438],[52.5046,13.3369],[52.5031,13.3274],[52.50175,13.329],[52.50344,13.32925],[52.5015,13.331],[52.50177,13.33299],[52.5029,13.3341],[52.50363,13.33526],[52.50503,13.3374],[52.5037,13.3378],[52.5044,13.3315],[52.50085,13.3338],[52.503,13.332],[52.5039,13.3315],[52.5035,13.33],[52.50043,13.32884],[52.5036,13.3398],[52.50485,13.33513],[52.50362,13.33195],[52.50717,13.33189],[52.504,13.332],[52.5041,13.3428],[52.5003,13.343],[52.49891,13.34488],[52.5033,13.3443],[52.502,13.347],[52.5018,13.3472],[52.50073,13.34899],[52.4992,13.3482],[52.5036,13.3473],[52.504,13.348],[52.5043,13.3486],[52.4978,13.34826],[52.49729,13.34958],[52.5024,13.3238],[52.5044,13.3236],[52.503,13.322],[52.5035,13.3197],[52.5021,13.3195],[52.50047,13.31694],[52.50064,13.31652],[52.5002,13.31946],[52.49843,13.31757],[52.5015,13.3217],[52.50421,13.31852],[52.5058,13.3187],[52.5042,13.3185],[52.5059,13.319],[52.502,13.321],[52.5027,13.322],[52.49973,13.32261],[52.50138,13.32217],[52.50378,13.32417],[52.50416,13.32387],[52.49995,13.32358],[52.49994,13.32176],[52.50012,13.32297],[52.5019,13.3219],[52.498,13.319],[52.49821,13.31867],[52.5,13.3192],[52.5017,13.3269],[52.50338888888889,13.33863888888889],[52.50416666666667,13.332777777777778],[52.504444444444445,13.33888888888889],[52.50194444444445,13.343055555555557],[52.50194444444445,13.342777777777778],[52.5025,13.325555555555555],[52.50550833333333,13.331383333333333],[52.504,13.331],[52.50472222222222,13.335555555555556],[52.505,13.335],[52.498333333333335,13.334166666666667],[52.507777777777775,13.331666666666667],[52.501666666666665,13.341111111111111],[52.50555555555555,13.340555555555556],[52.50527777777778,13.33888888888889],[52.50222222222222,13.340833333333334]],[[52.529605555555555,13.378361111111111],[52.5308,13.384],[52.5298,13.3839],[52.52864,13.37893],[52.5328,13.3802],[52.52891,13.3798],[52.5324,13.38076],[52.53277,13.38061],[52.53047,13.38207],[52.53002,13.37951],[52.529,13.376],[52.5284,13.3837],[52.53111111111111,13.3825]],[[52.5262,13.3876],[52.5237,13.3879],[52.5239,13.3819],[52.52027777777778,13.386944444444444],[52.52166666666667,13.386111111111111],[52.522,13.3846],[52.52142,13.38805],[52.51979,13.38835],[52.51784,13.38736],[52.5213,13.38502],[52.5208,13.3869],[52.5183,13.3887],[52.5223,13.3844],[52.5196,13.3884],[52.5166,13.3887],[52.52028,13.38694],[52.52444444444444,13.382222222222223],[52.525555555555556,13.387222222222222],[52.522777777777776,13.386111111111111],[52.51583333333333,13.386944444444444],[52.52027777777778,13.383333333333333],[52.520833333333336,13.38861111111111],[52.516666666666666,13.389166666666666],[52.52333333333333,13.383888888888889],[52.521635,13.386595],[52.514722222222225,13.389166666666666]],[[52.5031,13.3891],[52.50479,13.39095],[52.50877,13.39346],[52.5071,13.3905],[52.50833333333333,13.391666666666666],[52.51486,13.40285],[52.5183,13.4083],[52.5186,13.4081],[52.5168,13.4074],[52.51748,13.40745],[52.52003,13.40489],[52.5175,13.402777777777779],[52.51956,13.40281],[52.51932,13.39899],[52.52201,13.39424],[52.52177,13.39538],[52.5202,13.39774],[52.5197,13.3986],[52.5179,13.39703],[52.51078,13.39324],[52.5181,13.3933],[52.5121,13.3896],[52.51256,13.38954],[52.5123,13.3932],[52.51074,13.39174],[52.5148,13.3911],[52.5158,13.3937],[52.5123,13.393],[52.5132,13.3923],[52.5149,13.3911],[52.516,13.3946],[52.5171,13.3947],[52.5211,13.39669],[52.52063,13.39855],[52.51914,13.401],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.51667,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.51525,13.39741],[52.51565,13.3977],[52.519444444444446,13.406944444444445],[52.5183,13.3953],[52.51833333333333,13.404166666666667],[52.5231,13.3969],[52.519,13.398],[52.513888888888886,13.401388888888889],[52.516666666666666,13.39111111111111],[52.51833333333333,13.39611111111111],[52.520239,13.397741],[52.518055555555556,13.409444444444444],[52.517,13.411],[52.51972222222222,13.402777777777779],[52.51722222222222,13.402777777777779],[52.51694444444444,13.413055555555555],[52.520694444444445,13.396805555555556],[52.51638888888889,13.392777777777777],[52.5175,13.395555555555555],[52.51861111111111,13.408333333333333],[52.519444444444446,13.402777777777779],[52.516666666666666,13.395],[52.5175,13.402777777777779],[52.507777777777775,13.390555555555554],[52.51361111111111,13.392222222222221],[52.515277777777776,13.411944444444444],[52.52194444444444,13.39472222222222],[52.516666666666666,13.407222222222222],[52.505833333333335,13.390555555555554],[52.51861111111111,13.399722222222222],[52.519444444444446,13.398333333333333],[52.51222222222222,13.389444444444443],[52.51027777777778,13.390277777777778],[52.52131,13.39784],[52.51611111111111,13.398888888888889],[52.52055555555555,13.397777777777778],[52.51902777777778,13.402222222222223],[52.51722222222222,13.397777777777778],[52.51611111111111,13.412222222222223],[52.507777777777775,13.390555555555554],[52.5175,13.402777777777779],[52.5075,13.39027],[52.519,13.398],[52.52305555555555,13.396944444444443],[52.51777777777777,13.396944444444443],[52.519,13.398],[52.517,13.408],[52.521,13.396],[52.51361111111111,13.392777777777777],[52.52055555555555,13.406666666666666]],[[52.51809,13.38461],[52.51667,13.38333],[52.516952777777774,13.38341111111111],[52.516666666666666,13.383333333333333],[52.50507,13.38532],[52.50458,13.38318],[52.5033,13.3797],[52.5034,13.3801],[52.50396,13.38121],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.5186,13.376],[52.51862,13.376],[52.5138,13.379],[52.514,13.382],[52.51166666666666,13.381944444444445],[52.51598,13.37716],[52.51627,13.37769],[52.51,13.37611],[52.5092,13.3737],[52.5086,13.3768],[52.5108,13.3753],[52.50888,13.37852],[52.50931,13.37765],[52.5125,13.3815],[52.5179,13.3759],[52.5179,13.3759],[52.51605,13.38015],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.505,13.381944444444445],[52.5063,13.3797],[52.51583333333333,13.380833333333333],[52.51583333333333,13.380555555555556],[52.51325277777778,13.376316666666668],[52.515277777777776,13.378333333333334],[52.505,13.377500000000001],[52.51658,13.381],[52.5116504028,13.38398695],[52.51555555555556,13.37888888888889],[52.51111111111111,13.38],[52.515,13.379444444444445],[52.51166666666666,13.384722222222221],[52.507222222222225,13.3825],[52.50944444444445,13.375833333333334],[52.51638888888889,13.37888888888889],[52.51,13.382777777777779],[52.51658,13.381],[52.508697222222224,13.384],[52.51638888888889,13.37888888888889],[52.508697222222224,13.384],[52.50388888888889,13.375],[52.506388888888885,13.381944444444445],[52.50805555555556,13.381944444444445],[52.5094,13.3765],[52.50694444444444,13.382777777777779],[52.513333333333335,13.377222222222223],[52.51,13.3744444444],[52.50555555555555,13.37638888888889],[52.5125,13.38361111111111],[52.5175,13.381388888888889],[52.50972222222222,13.384166666666665],[52.51,13.379],[52.51861111111111,13.376111111111111],[52.519635,13.376831],[52.50611111111111,13.385555555555555],[52.51627222222222,13.377722222222223],[52.51638888888889,13.380833333333333],[52.504444444444445,13.383055555555556],[52.51,13.373611111111112]],[[52.45616,13.2919],[52.45721,13.2895],[52.4557,13.29446],[52.45647,13.29295],[52.45836,13.28861],[52.457,13.292],[52.45666666666667,13.291944444444445],[52.4575,13.289722222222222],[52.45583333333334,13.2925],[52.4581,13.28702],[52.4558333333,13.2927777778]]]
	     */
	    // area around betahaus
	    double[][] polygonPoints=new double[][] {
	            {52.502286,13.412372},
	            {52.49906,13.418273},
	            {52.503618,13.410741},
	            {52.498589,13.404884},
	            {52.506178,13.403854},
	            {52.502743,13.424474},
	            {52.498354,13.413166},
	            {52.502913,13.407909},
	            {52.50011,13.30723},
	            {52.49632,13.32024},
	            {52.4957,13.3033}

	            // not in the area

	    };
	    //,{52.49939,13.30523},{52.5004,13.312},{52.50011,13.30723},{52.49907,13.31225},{52.50019,13.30983},{52.49984,13.30646},{52.4968,13.3053},{52.5002,13.3103},{52.50083333333333,13.312777777777779},{52.5,13.307222222222222}};

        double[][] polygon = GeoGeometry.polygonForPoints(polygonPoints);
//        double[][] polygon = ConvexHull.cvxHull(polygonPoints);

	    StringBuilder buf = new StringBuilder();
	    buf.append("[");
	    for (double[] p : polygon) {
	        buf.append("["+p[0]+","+p[1]+"],");
        }
	    buf.deleteCharAt(buf.length()-1);
	    buf.append("]");

	    // FIXME complete test
	}

	public void polygonForPointsInFourQuadrantsShouldContainStuffInside() {
	    double[][] polygon = GeoGeometry.polygonForPoints(new double[][]{sydney,newyork,amsterdam,buenosaires});
        assertThat("should be inside", polygonContains(london, polygon));
        assertThat("should NOT be inside", !polygonContains(berlin, polygon));
	}

   public void shouldConvertCircleToPolygonOn180() {
        double[][] circle2polygon = GeoGeometry.circle2polygon(6, -18, 180, 1000);
        int countEast=0;
        for (double[] point : circle2polygon) {
            double distance = distance(-18, 180,point[1],point[0]);
            assertThat(abs(1000-distance), lessThan(1.0));
            if(GeoHashUtils.isWest(180, point[0])) {
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
            double distance = distance(lat, lon,point[1],point[0]);
            assertThat(abs(1000-distance), lessThan(20.0));
            if(GeoHashUtils.isWest(180, point[0])) {
                countEast++;
            }
        }
        assertThat(countEast, greaterThan(1));
    }

   @DataProvider
   public Object[][] degrees() {
       return new Object[][] {
               {"W",111,38,45.40, -111.64594444444445},
               {"E",111,38,45.40, 111.64594444444445}
       };

   }

    @Test(dataProvider = "degrees")
    public void shouldConvertToDecimalDegree(String direction, double degrees, double minutes, double seconds, double expected) {
        double decimalDegree = GeoGeometry.toDecimalDegree(direction, degrees, minutes, seconds);
        assertThat(decimalDegree, is(expected));
    }

    public void shouldFilterPoints() {
        double latitude=52.0;
        double longitude=13.0;
        double points[][] = new double[1000][2];
        Random random = new Random();
        for(int i=0;i<1000;i++) {
            points[i]=new double[] {longitude+random.nextDouble(),latitude+random.nextDouble()};
        }
        // insert a few 'bad' points
        points[50]=new double[]{100,100};
        points[100]=new double[]{-100,100};
        points[150]=new double[]{100,-100};
        points[200]=new double[]{-100,-100};

        double[][] filtered = GeoGeometry.filterNoiseFromPointCloud(points, 0.005f);

        assertThat(filtered.length, is(996));
        double[] bbox = GeoGeometry.boundingBox(filtered);

        // the four bad pois should be gone
        assertThat(bbox[0], allOf(greaterThan(52.0), lessThan(53.0)));
        assertThat(bbox[1], allOf(greaterThan(52.0), lessThan(53.0)));
        assertThat(bbox[2], allOf(greaterThan(13.0), lessThan(14.0)));
        assertThat(bbox[3], allOf(greaterThan(13.0), lessThan(14.0)));
    }

    public void shouldOverLapWithSelf() {
        assertThat("should overlap with itself",GeoGeometry.overlap(samplePolygon, samplePolygon));
    }

    @DataProvider
    public Object[][] overlappingPolygons() {
        return new Object[][] {
                {new double[][]{berlin, amsterdam, newyork}, new double[][]{london,potsDammerPlatz,moritzPlatz}},
                {new double[][]{rosenthalerPlatz,moritzPlatz,brandenBurgerGate}, new double[][]{oranienburgerTor,potsDammerPlatz,senefelderPlatz}}
        };
    }

    @Test(dataProvider="overlappingPolygons")
    public void shouldOverlap(double[][] left, double[][] right) {
        assertThat("left should overlap with right",GeoGeometry.overlap(left,right));
        assertThat("right should overlap with left",GeoGeometry.overlap(right, left));
    }

    public void shouldExpandPolygon() {
        double[][] expandPolygon = GeoGeometry.expandPolygon(2000, samplePolygon);
        assertThat("expanded polygon should contain polygon", GeoGeometry.contains(expandPolygon, samplePolygon));
    }

    public void shouldBeRight() {
         assertThat("should be right", GeoGeometry.rightTurn(new double[]{1,1},new double[] {2,2}, new double[] {1,10}));
         assertThat("should be right", GeoGeometry.rightTurn(new double[]{1,1},new double[] {2,2}, new double[] {3,4}));
         assertThat("should not be right", !GeoGeometry.rightTurn(new double[]{1,1},new double[] {0,2}, new double[] {1,10}));
         assertThat("should not be right", !GeoGeometry.rightTurn(new double[]{1,1},new double[] {1,2}, new double[] {1,10}));
    }

    @DataProvider
    public Object[][] clouds() {
        return new Object[][] {
                {new double[][] {{1,1},{2,2},{3,3},{3,0},{0,3}}, new double[][] {{2,2}} },
                {new double[][] {{1,1},{-1,-1},{-1,1},{1,-1},{0,0}}, new double[][] {{0,0}} }
        };
    }

    @Test(dataProvider="clouds")
    public void shouldCalculatePolygonForPointCloud(double[][] points, double[][] contained) {
        double[][] polygonForPoints = GeoGeometry.polygonForPoints(points);
        for (int i = 0; i < contained.length; i++) {
            assertThat("point in the middle should be contained", polygonContains(contained[i], polygonForPoints));
        }
    }

    public void shouldCalculateCorrectBbox() {
        double[] bbox = GeoGeometry.bbox(berlin[1], berlin[0], 1000, 1000);
        assertThat(distance(bbox[0],berlin[0],berlin[1],berlin[0]), greaterThan(499.0));
        assertThat(distance(bbox[0],berlin[0],berlin[1],berlin[0]), lessThan(501.0));
        assertThat(distance(bbox[1],berlin[0],berlin[1],berlin[0]), greaterThan(499.0));
        assertThat(distance(bbox[1],berlin[0],berlin[1],berlin[0]), lessThan(501.0));
        assertThat(distance(berlin[1],bbox[2],berlin[1],berlin[0]), greaterThan(499.0));
        assertThat(distance(berlin[1],bbox[2],berlin[1],berlin[0]), lessThan(501.0));
        assertThat(distance(berlin[1],bbox[3],berlin[1],berlin[0]), greaterThan(499.0));
        assertThat(distance(berlin[1],bbox[3],berlin[1],berlin[0]), lessThan(501.0));
    }

    public void shouldCalculateBboxForPoint() {
        double[] bbox = boundingBox(new double[]{13,52});
        assertThat(bbox[0], is(52.0));
        assertThat(bbox[1], is(52.0));
        assertThat(bbox[2], is(13.0));
        assertThat(bbox[3], is(13.0));
    }

    public void shouldCalculateBboxForLinePolygonMultiPolygon() {
        double[][] line = circle2polygon(5000, 52, 13, 1000);
        double[][][] polygon = new double[][][] {line};
        double[][][][] multiPolygon = new double[][][][] {polygon};
        assertThat(area(boundingBox(line)), is(area(boundingBox(polygon))));
        assertThat(area(boundingBox(line)), is(area(boundingBox(multiPolygon))));
    }

    public void shouldCalculateCorrectPolygonForBbox() {
        double[] bbox = GeoGeometry.bbox(berlin[1], berlin[0], 1000, 1000);
        double[][] polygon = GeoGeometry.bbox2polygon(bbox);
        assertThat(polygon.length, is(5));
        assertThat(polygon[0][0], is(polygon[polygon.length-1][0]));
        assertThat(polygon[0][1], is(polygon[polygon.length-1][1]));
    }

    @DataProvider
    public Object[][] linesNPoints() {
        return new Object[][] {
                {52.520316,13.414654, 52.528149,13.423709, 52.52392,13.412122, 416l},
                {52.521337,13.403108,52.517002,13.409073,52.519039,13.408665,138l},
                {52.471551,13.385791,52.478139,13.385791,52.476244,13.384718,73l},//vertical
                {52.476244,13.384718,52.476244,14,52.478139,13.385062,211l},// horizontal
                {1,1,3,3,2.1,2.1,0l},
                {1,1,3,1,2,1,0l},
                {1,1,1,3,1,2,0l},
                {1,1,3,3,2.0,2.1,7860l},
                {1,1,3,3,90,90,distance(3,3,90,90)},
                {1,1,3,3,0,0,distance(1,1,0,0)},
        };
    }

    @Test(dataProvider="linesNPoints")
    public void shouldCalculateDistanceToLine(double x1, double y1, double x2, double y2, double px, double py, double expectedDistance) {
        double distance = GeoGeometry.distance(x1,y1,x2,y2,px,py);
        double distance2 = GeoGeometry.distance(new double[]{y1,x1},new double[]{y2,x2},new double[]{py,px});
        assertThat(distance, is(distance2));
        assertThat(round(distance), is(round(expectedDistance)));
        double distToPoint1 = distance(x1,y1,px,py);
        double distToPoint2 = distance(x1,y1,px,py);


        assertThat("",distance, lessThanOrEqualTo(distToPoint1));
        assertThat("",distance, lessThanOrEqualTo(distToPoint2));
    }

    public void shouldCalculateDistanceToLineString() {
        double[][] ls = new double[][] {{13.414654,52.520316},{13.423709,52.528149},{13.425724,52.524992}};
        long d = Math.round(GeoGeometry.distanceToLineString(new double[] {13.412122,52.52392}, ls));
        assertThat(d, is(416l));
        double[][] ls2 = new double[][] {{13.425724,52.524992},{13.414654,52.520316},{13.423709,52.528149}};
        d = Math.round(GeoGeometry.distanceToLineString(new double[] {13.412122,52.52392}, ls2));
        assertThat(d, is(416l));
    }

    public void shouldCalculateDistanceToPolygon() {
        double[][] polygon = new double[][] {{13.414654,52.520316},{13.423709,52.528149},{13.425724,52.524992},{13.414654,52.520316}};
        long d = Math.round(GeoGeometry.distanceToPolygon(new double[] {13.412122,52.52392}, polygon));
        assertThat(d, is(416l));
        double[][] polygon2 = new double[][] {{13.425724,52.524992},{13.414654,52.520316},{13.423709,52.528149},{13.425724,52.524992}};
        d = Math.round(GeoGeometry.distanceToPolygon(new double[] {13.412122,52.52392}, polygon2));
        assertThat(d, is(416l));
        d = Math.round(GeoGeometry.distanceToPolygon(new double[] {13.412122,52.52392}, new double[][][] {polygon2}));
        assertThat(d, is(416l));
        double[] center = GeoGeometry.polygonCenter(polygon);
        d = Math.round(GeoGeometry.distanceToPolygon(center, polygon2));
        assertThat(d, is(0l));
    }

    public void shouldCalculateDistanceToMultiPolygon() {
        double[][] polygon = new double[][] {{13.414654,52.520316},{13.423709,52.528149},{13.425724,52.524992},{13.414654,52.520316}};
        double[][][][] multiPolygon = new double[][][][] {{polygon},{{{1,1},{1,2},{2,2},{1,1}}}};
        double[][][][] multiPolygon2 = new double[][][][] {{{{1,1},{1,2},{2,2},{1,1}}},{polygon}};
        long d = Math.round(GeoGeometry.distanceToMultiPolygon(new double[] {13.412122,52.52392}, multiPolygon));
        assertThat(d, is(416l));
        d = Math.round(GeoGeometry.distanceToMultiPolygon(new double[] {13.412122,52.52392}, multiPolygon2));
        assertThat(d, is(416l));
    }

    public void shouldCalculateArea() {
        double[][] circle = GeoGeometry.circle2polygon(5000, 52, 13, 1000);
        double calculatedArea = GeoGeometry.area(circle);
        double circleArea = Math.PI*1000*1000;

        assertThat("0.005% difference allowed perfect circle area and calculated area",Math.abs(circleArea-calculatedArea), lessThan(calculatedArea/200));
    }

    public void shouldCalculateAreaForBbox() {
        int radius = 1000;
        double[] bboxForCircle = boundingBox(circle2polygon(5000, 52, 13, radius));
        double area = area(bboxForCircle);
        double expectedArea=radius*2*radius*2;
        assertThat("area should be roughly similar to that of the ideal rectangle of 2000x2000",abs(area-expectedArea), lessThan(expectedArea*0.01));
    }

    public void shouldCalculateAreaOfPolygonWithHole() {
        double[][] outer = GeoGeometry.circle2polygon(5000, 52, 13, 1000);
        double[][] inner = GeoGeometry.circle2polygon(5000, 52, 13, 1000);
        double[][][] polygon=new double[][][] {outer,inner};
        assertThat(area(polygon), is(area(outer)-area(inner)));
    }

    public void shouldCalculateAreaOfMultiPolygon() {
        double[][] outer = GeoGeometry.circle2polygon(5000, 52, 13, 1000);
        double[][] inner = GeoGeometry.circle2polygon(5000, 52, 13, 1000);
        double[][][] polygon=new double[][][] {outer,inner};
        double[][][][] multiPolygon = new double[][][][] {polygon,polygon};
        assertThat(area(multiPolygon), is(2*area(polygon)));
    }

    public void shouldSimplifyLineOnlyOnce() {
        double[][] line=new double[][] {newyork,moritzPlatz,senefelderPlatz,naturkundeMuseum, buenosaires};
        double[][] simplified = GeoGeometry.simplifyLine(line, 10000);
        double[][] superSimplified = GeoGeometry.simplifyLine(simplified,10000);
        assertThat(simplified.length, lessThan(line.length));
        assertThat(simplified.length, is(superSimplified.length));
    }

    @DataProvider
    public Object[][] straightLines() {
        return new Object[][] {
                {new double[][] {{0,1},{1,1},{2,1},{3,1},{4,1}}},
                {new double[][] {{1,0},{1,1},{1,2},{1,3},{1,4}}},
                {new double[][] {{0,0},{1,1},{2,2},{3,3},{4,4}}},
                {new double[][] {{0,0},{1,1},{2,2},{4,4}}},
                {new double[][] {{0,0},{1,1},{4,4}}},
                {new double[][] {{0,0},{1,1}}},

        };
    }

    @Test(dataProvider="straightLines")
    public void shouldSimplifyStraightLines(double[][] line) {
        double[][] simplified = GeoGeometry.simplifyLine(line, 1);
        assertThat(simplified.length, is(2));
    }

    public void shouldSimplifyCircle() {
        double[][] poly = GeoGeometry.circle2polygon(10000, 52, 13, 1000); // 10000 segments!
        double[][] simplified = GeoGeometry.simplifyLine(poly, 100);
        assertThat(simplified.length, lessThan(poly.length));
    }

    @DataProvider
    public Object[][] validPoints() {
        return new Object[][] {
                {amsterdam},
                {berlin},
                {new double[]{180.0000004999,52}},// allow small rounding 'errors'
                {new double[]{180.0000004999,90.0000004999}}
        };
    }

    @Test(dataProvider="validPoints")
    public void shouldValidate(double[] point) {
        // should throw no exceptions
        GeoGeometry.validate(point);
    }

    @DataProvider
    public Object[][] invalidPoints() {
        return new Object[][] {
                {new double[]{180.000001,0}},
                {new double[]{-180.000001,0}},
                {new double[]{0,90.000001}},
                {new double[]{0,-90.000001}}
        };
    }


    @Test(dataProvider="invalidPoints", expectedExceptions=IllegalArgumentException.class)
    public void shouldNotValidate(double[] point) {
        // should throw exceptions
        GeoGeometry.validate(point);
    }

    public void issue5PolygonContainsDoesNotWorkCorrectly() {
        double[][][] polygon = new double[][][] { { { 42.503320312499994, 1.7060546875 }, { 42.4966796875, 1.678515625000017 },
                { 42.455957031249994, 1.58642578125 }, { 42.441699218749996, 1.534082031250023 }, { 42.434472656249994, 1.486230468750023 },
                { 42.437451171875, 1.448828125 }, { 42.46132812499999, 1.428125 }, { 42.497851562499996, 1.430273437500006 },
                { 42.530810546874996, 1.421972656250006 }, { 42.548388671874996, 1.414843750000017 }, { 42.5958984375, 1.428320312500006 },
                { 42.6216796875, 1.458886718750023 }, { 42.642724609374994, 1.501367187500023 }, { 42.635009765625, 1.568164062500017 },
                { 42.604443359375, 1.709863281250023 }, { 42.575927734375, 1.739453125000011 }, { 42.55673828125, 1.740234375 },
                { 42.525634765625, 1.713964843750006 }, { 42.503320312499994, 1.7060546875 } } };
        double[] point=new double[] {42.503615,1.641881};

        assertThat("should contain the point", GeoGeometry.polygonContains(point, polygon));
    }
}
