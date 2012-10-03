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
	    double[][] polygon = GeoGeometry.getPolygonForPoints(placesInMitte);

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
	    
        double[][] polygon = GeoGeometry.getPolygonForPoints(polygonPoints);
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
	    double[][] polygon = GeoGeometry.getPolygonForPoints(new double[][]{sydney,newyork,amsterdam,buenosaires});
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
               {"W",111,38,45.40, -111.64594444444445},
               {"E",111,38,45.40, 111.64594444444445}
       };

   }

    @Test(dataProvider = "degrees")
    public void shouldConvertToDecimalDegree(String direction, double degrees, double minutes, double seconds, double expected) {
        double decimalDegree = GeoGeometry.toDecimalDegree(direction, degrees, minutes, seconds);
        assertThat(decimalDegree, is(expected));

    }
}
