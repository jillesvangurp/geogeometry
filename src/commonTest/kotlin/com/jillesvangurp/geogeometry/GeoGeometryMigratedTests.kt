package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.GeoGeometry.Companion.area
import com.jillesvangurp.geo.GeoGeometry.Companion.bbox
import com.jillesvangurp.geo.GeoGeometry.Companion.bboxContains
import com.jillesvangurp.geo.GeoGeometry.Companion.boundingBox
import com.jillesvangurp.geo.GeoGeometry.Companion.circle2polygon
import com.jillesvangurp.geo.GeoGeometry.Companion.contains
import com.jillesvangurp.geo.GeoGeometry.Companion.distance
import com.jillesvangurp.geo.GeoGeometry.Companion.distanceToLineString
import com.jillesvangurp.geo.GeoGeometry.Companion.distanceToMultiPolygon
import com.jillesvangurp.geo.GeoGeometry.Companion.distanceToPolygon
import com.jillesvangurp.geo.GeoGeometry.Companion.expandPolygon
import com.jillesvangurp.geo.GeoGeometry.Companion.filterNoiseFromPointCloud
import com.jillesvangurp.geo.GeoGeometry.Companion.linesCross
import com.jillesvangurp.geo.GeoGeometry.Companion.overlap
import com.jillesvangurp.geo.GeoGeometry.Companion.polygonCenter
import com.jillesvangurp.geo.GeoGeometry.Companion.polygonContains
import com.jillesvangurp.geo.GeoGeometry.Companion.polygonForPoints
import com.jillesvangurp.geo.GeoGeometry.Companion.rightTurn
import com.jillesvangurp.geo.GeoGeometry.Companion.roundToDecimals
import com.jillesvangurp.geo.GeoGeometry.Companion.simplifyLine
import com.jillesvangurp.geo.GeoGeometry.Companion.validate
import com.jillesvangurp.geo.GeoGeometry.Companion.vicentyDistance
import com.jillesvangurp.geo.GeoHashUtils.Companion.isWest
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.round
import kotlin.random.Random
import kotlin.test.Test


class GeoGeometryMigratedTests {
    val bergstr16InvalidenBerlin = doubleArrayOf(13.393674, 52.5310059)
    val bergstr16Berlin = doubleArrayOf(13.3941763, 52.5298311)
    val berlin = doubleArrayOf(13.385721, 52.527109)
    val sydney = doubleArrayOf(151.206146, -33.872796)
    val buenosaires = doubleArrayOf(-58.380449, -34.602875)
    val newyork = doubleArrayOf(-74.011237, 40.721119)
    val amsterdam = doubleArrayOf(4.894252, 52.372103)
    val london = doubleArrayOf(-0.123656, 51.51283)

    val brandenBurgerGate = doubleArrayOf(13.377157, 52.516279)
    val potsDammerPlatz = doubleArrayOf(13.376599, 52.509515)
    val moritzPlatz = doubleArrayOf(13.410717, 52.503663)
    val senefelderPlatz = doubleArrayOf(13.412949, 52.532755)
    val naturkundeMuseum = doubleArrayOf(13.381921, 52.531188)
    val rosenthalerPlatz = doubleArrayOf(13.401361, 52.529948)
    val oranienburgerTor = doubleArrayOf(13.38707, 52.525339)

    val samplePolygon = arrayOf(
        doubleArrayOf(1.0, 1.0),
        doubleArrayOf(1.0, -1.0),
        doubleArrayOf(-1.0, -1.0),
        doubleArrayOf(-1.0, 1.0)
    )

    @Test
    fun shouldCheckThatLinesCross() {
        // should intersect
        linesCross(1.0, 1.0, 2.0, 2.0, 1.0, 2.0, 2.0, 1.0) shouldBe true
        // should intersect (vertical)",
        linesCross(1.0, 1.0, 1.0, 10.0, 1.0, 3.0, 1.0, 4.0) shouldBe true
        // should intersect (horizontal)",
        linesCross(1.0, 666.0, 10.0, 666.0, 3.0, 666.0, 4.0, 666.0) shouldBe true
    }

    @Test
    fun shouldCheckThatLinesDontCross() {
        linesCross(1.0, 2.0, 3.0, 4.0, 10.0, 20.0, 20.0, 10.0) shouldNotBe true
        linesCross(1.0, 1.0, 2.0, 2.0, 2.1, 2.1, 3.0, 3.0) shouldNotBe true
        linesCross(1.0, 1.0, 1.0, 5.0, 1.0, 6.0, 1.0, 10.0) shouldNotBe true
        linesCross(1.0, 666.0, 5.0, 666.0, 6.0, 666.0, 10.0, 666.0) shouldNotBe true

    }

    @Test
    fun shouldCheckContainmentForPolygonCorrectly() {
        // origin should be in there
        polygonContains(0.0, 0.0, *samplePolygon) shouldBe true
        // should be outside
        !polygonContains(20.0, 20.0, *samplePolygon) shouldBe true
        // should be inside
        polygonContains(0.5, 0.5, *samplePolygon) shouldBe true
        // should be inside
        polygonContains(0.5, -0.5, *samplePolygon) shouldBe true
        // should be inside
        polygonContains(-0.5, 0.5, *samplePolygon) shouldBe true
        // should be inside
        polygonContains(-0.5, -0.5, *samplePolygon) shouldBe true
    }

    @Test
    fun shouldBboxContainPoint() {
        val southLatitude = -1.0
        val northLatitude = 1.0
        val westLongitude = -2.0
        val eastLongitude = 2.0
        val bbox = doubleArrayOf(westLongitude, southLatitude, eastLongitude, northLatitude)

        withClue("should be contained") { bboxContains(bbox, 0.0, 0.0) shouldBe true }
        withClue("should be contained") { bboxContains(bbox, 1.0, 2.0) shouldBe true }
        withClue("should be contained") { bboxContains(bbox, -1.0, 2.0) shouldBe true }
        withClue("should be contained") { bboxContains(bbox, 1.0, 2.0) shouldBe true }
        withClue("should be contained") { bboxContains(bbox, -1.0, -2.0) shouldBe true }
        withClue("should not be contained") { bboxContains(bbox, -1.1, -2.1) shouldBe false }

    }

    @Test
    fun shouldGetCorrectBboxForPolygon() {
        val bbox = boundingBox(samplePolygon)
        withClue("should be contained") { bboxContains(bbox, 0.0, 0.0) shouldBe true }
        for (coordinate in samplePolygon) {
            withClue("should contain point") {
                bboxContains(bbox, coordinate[1], coordinate[0]) shouldBe true
            }
            withClue("should not contain point") {
                bboxContains(bbox, 20 * coordinate[1], 20 * coordinate[0]) shouldBe false
            }
        }

    }

    @Test
    fun shouldRoundDouble() {
        roundToDecimals(0.1234567891111112, 17) shouldBe 0.1234567891111112

    }

    @Test
    fun shouldCalculateDistance() {
        distance(sydney, berlin).toLong() shouldBe 16_095_663
        // vicenty is a bit more accurate by about 7km
        vicentyDistance(sydney, berlin).toLong() shouldBe 16_089_576
    }

    @Test
    fun shouldCalculateShortDistance() {
        round(distance(bergstr16Berlin, bergstr16InvalidenBerlin)) shouldBe 135.0
        // no difference over short distances at this latitude
        round(vicentyDistance(bergstr16Berlin, bergstr16InvalidenBerlin)) shouldBe 135.0
    }

    @Test
    fun shouldHaveDistanceOfRadiusForEachPoint() {
        val radius = 50000
        val segments = 500
        val polygon =
            circle2polygon(segments, london[1], london[0], radius.toDouble())
        var d = 0.0
        var last: DoubleArray? = null
        for (point in polygon[0]) {
            if (last != null) {
                d += distance(last, point)
            }
            val distance =
                distance(doubleArrayOf(london[0], london[1]), point)
            val difference = abs(radius - distance)
            withClue(difference) {
                difference shouldBeLessThan 100.0
            }
            last = point
        }
        // close the circle
        d += distance(polygon[0][0], last!!)
        val difference = abs(d - 2 * PI * radius)
        difference shouldBeLessThan 100.0
    }

    @Test
    fun polygonForPointsShouldContainStuffInside() {
        val placesInMitte = arrayOf(
            brandenBurgerGate, potsDammerPlatz, moritzPlatz, senefelderPlatz, naturkundeMuseum
        )
        val polygon = polygonForPoints(placesInMitte)
        polygonContains(rosenthalerPlatz[1], rosenthalerPlatz[0], *polygon) shouldBe true
        polygonContains(oranienburgerTor[1], oranienburgerTor[0], *polygon) shouldBe true
        polygonContains(1.0, 1.0, *polygon) shouldBe false
    }

    @Test
    fun shouldCreateSensiblePolygon() {
        /*
         * [[[52.49768,13.31191],[52.499,13.313],[52.5007,13.3065],[52.4957,13.3033],[52.49939,13.30523],[52.5004,13.312],[52.50011,13.30723],[52.49907,13.31225],[52.50019,13.30983],[52.49984,13.30646],[52.4968,13.3053],[52.5002,13.3103],[52.50083333333333,13.312777777777779],[52.5,13.307222222222222]],
         * [[52.49878,13.33396],[52.49604,13.31996],[52.50045,13.31918],[52.49632,13.32024],[52.49983,13.34196],[52.503,13.3316],[52.50277,13.32743],[52.5005,13.3288],[52.50272,13.32918],[52.5029,13.3275],[52.5025,13.3291],[52.5027,13.332],[52.507,13.347],[52.5063,13.3445],[52.5044,13.3428],[52.505,13.339],[52.505,13.335],[52.50444,13.33889],[52.5017,13.3411],[52.50394,13.34048],[52.50624,13.34407],[52.506,13.33193],[52.50456,13.33438],[52.5046,13.3369],[52.5031,13.3274],[52.50175,13.329],[52.50344,13.32925],[52.5015,13.331],[52.50177,13.33299],[52.5029,13.3341],[52.50363,13.33526],[52.50503,13.3374],[52.5037,13.3378],[52.5044,13.3315],[52.50085,13.3338],[52.503,13.332],[52.5039,13.3315],[52.5035,13.33],[52.50043,13.32884],[52.5036,13.3398],[52.50485,13.33513],[52.50362,13.33195],[52.50717,13.33189],[52.504,13.332],[52.5041,13.3428],[52.5003,13.343],[52.49891,13.34488],[52.5033,13.3443],[52.502,13.347],[52.5018,13.3472],[52.50073,13.34899],[52.4992,13.3482],[52.5036,13.3473],[52.504,13.348],[52.5043,13.3486],[52.4978,13.34826],[52.49729,13.34958],[52.5024,13.3238],[52.5044,13.3236],[52.503,13.322],[52.5035,13.3197],[52.5021,13.3195],[52.50047,13.31694],[52.50064,13.31652],[52.5002,13.31946],[52.49843,13.31757],[52.5015,13.3217],[52.50421,13.31852],[52.5058,13.3187],[52.5042,13.3185],[52.5059,13.319],[52.502,13.321],[52.5027,13.322],[52.49973,13.32261],[52.50138,13.32217],[52.50378,13.32417],[52.50416,13.32387],[52.49995,13.32358],[52.49994,13.32176],[52.50012,13.32297],[52.5019,13.3219],[52.498,13.319],[52.49821,13.31867],[52.5,13.3192],[52.5017,13.3269],[52.50338888888889,13.33863888888889],[52.50416666666667,13.332777777777778],[52.504444444444445,13.33888888888889],[52.50194444444445,13.343055555555557],[52.50194444444445,13.342777777777778],[52.5025,13.325555555555555],[52.50550833333333,13.331383333333333],[52.504,13.331],[52.50472222222222,13.335555555555556],[52.505,13.335],[52.498333333333335,13.334166666666667],[52.507777777777775,13.331666666666667],[52.501666666666665,13.341111111111111],[52.50555555555555,13.340555555555556],[52.50527777777778,13.33888888888889],[52.50222222222222,13.340833333333334]],[[52.529605555555555,13.378361111111111],[52.5308,13.384],[52.5298,13.3839],[52.52864,13.37893],[52.5328,13.3802],[52.52891,13.3798],[52.5324,13.38076],[52.53277,13.38061],[52.53047,13.38207],[52.53002,13.37951],[52.529,13.376],[52.5284,13.3837],[52.53111111111111,13.3825]],[[52.5262,13.3876],[52.5237,13.3879],[52.5239,13.3819],[52.52027777777778,13.386944444444444],[52.52166666666667,13.386111111111111],[52.522,13.3846],[52.52142,13.38805],[52.51979,13.38835],[52.51784,13.38736],[52.5213,13.38502],[52.5208,13.3869],[52.5183,13.3887],[52.5223,13.3844],[52.5196,13.3884],[52.5166,13.3887],[52.52028,13.38694],[52.52444444444444,13.382222222222223],[52.525555555555556,13.387222222222222],[52.522777777777776,13.386111111111111],[52.51583333333333,13.386944444444444],[52.52027777777778,13.383333333333333],[52.520833333333336,13.38861111111111],[52.516666666666666,13.389166666666666],[52.52333333333333,13.383888888888889],[52.521635,13.386595],[52.514722222222225,13.389166666666666]],[[52.5031,13.3891],[52.50479,13.39095],[52.50877,13.39346],[52.5071,13.3905],[52.50833333333333,13.391666666666666],[52.51486,13.40285],[52.5183,13.4083],[52.5186,13.4081],[52.5168,13.4074],[52.51748,13.40745],[52.52003,13.40489],[52.5175,13.402777777777779],[52.51956,13.40281],[52.51932,13.39899],[52.52201,13.39424],[52.52177,13.39538],[52.5202,13.39774],[52.5197,13.3986],[52.5179,13.39703],[52.51078,13.39324],[52.5181,13.3933],[52.5121,13.3896],[52.51256,13.38954],[52.5123,13.3932],[52.51074,13.39174],[52.5148,13.3911],[52.5158,13.3937],[52.5123,13.393],[52.5132,13.3923],[52.5149,13.3911],[52.516,13.3946],[52.5171,13.3947],[52.5211,13.39669],[52.52063,13.39855],[52.51914,13.401],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.51667,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.516666666666666,13.4],[52.51525,13.39741],[52.51565,13.3977],[52.519444444444446,13.406944444444445],[52.5183,13.3953],[52.51833333333333,13.404166666666667],[52.5231,13.3969],[52.519,13.398],[52.513888888888886,13.401388888888889],[52.516666666666666,13.39111111111111],[52.51833333333333,13.39611111111111],[52.520239,13.397741],[52.518055555555556,13.409444444444444],[52.517,13.411],[52.51972222222222,13.402777777777779],[52.51722222222222,13.402777777777779],[52.51694444444444,13.413055555555555],[52.520694444444445,13.396805555555556],[52.51638888888889,13.392777777777777],[52.5175,13.395555555555555],[52.51861111111111,13.408333333333333],[52.519444444444446,13.402777777777779],[52.516666666666666,13.395],[52.5175,13.402777777777779],[52.507777777777775,13.390555555555554],[52.51361111111111,13.392222222222221],[52.515277777777776,13.411944444444444],[52.52194444444444,13.39472222222222],[52.516666666666666,13.407222222222222],[52.505833333333335,13.390555555555554],[52.51861111111111,13.399722222222222],[52.519444444444446,13.398333333333333],[52.51222222222222,13.389444444444443],[52.51027777777778,13.390277777777778],[52.52131,13.39784],[52.51611111111111,13.398888888888889],[52.52055555555555,13.397777777777778],[52.51902777777778,13.402222222222223],[52.51722222222222,13.397777777777778],[52.51611111111111,13.412222222222223],[52.507777777777775,13.390555555555554],[52.5175,13.402777777777779],[52.5075,13.39027],[52.519,13.398],[52.52305555555555,13.396944444444443],[52.51777777777777,13.396944444444443],[52.519,13.398],[52.517,13.408],[52.521,13.396],[52.51361111111111,13.392777777777777],[52.52055555555555,13.406666666666666]],[[52.51809,13.38461],[52.51667,13.38333],[52.516952777777774,13.38341111111111],[52.516666666666666,13.383333333333333],[52.50507,13.38532],[52.50458,13.38318],[52.5033,13.3797],[52.5034,13.3801],[52.50396,13.38121],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.5186,13.376],[52.51862,13.376],[52.5138,13.379],[52.514,13.382],[52.51166666666666,13.381944444444445],[52.51598,13.37716],[52.51627,13.37769],[52.51,13.37611],[52.5092,13.3737],[52.5086,13.3768],[52.5108,13.3753],[52.50888,13.37852],[52.50931,13.37765],[52.5125,13.3815],[52.5179,13.3759],[52.5179,13.3759],[52.51605,13.38015],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.507222222222225,13.3825],[52.505,13.381944444444445],[52.5063,13.3797],[52.51583333333333,13.380833333333333],[52.51583333333333,13.380555555555556],[52.51325277777778,13.376316666666668],[52.515277777777776,13.378333333333334],[52.505,13.377500000000001],[52.51658,13.381],[52.5116504028,13.38398695],[52.51555555555556,13.37888888888889],[52.51111111111111,13.38],[52.515,13.379444444444445],[52.51166666666666,13.384722222222221],[52.507222222222225,13.3825],[52.50944444444445,13.375833333333334],[52.51638888888889,13.37888888888889],[52.51,13.382777777777779],[52.51658,13.381],[52.508697222222224,13.384],[52.51638888888889,13.37888888888889],[52.508697222222224,13.384],[52.50388888888889,13.375],[52.506388888888885,13.381944444444445],[52.50805555555556,13.381944444444445],[52.5094,13.3765],[52.50694444444444,13.382777777777779],[52.513333333333335,13.377222222222223],[52.51,13.3744444444],[52.50555555555555,13.37638888888889],[52.5125,13.38361111111111],[52.5175,13.381388888888889],[52.50972222222222,13.384166666666665],[52.51,13.379],[52.51861111111111,13.376111111111111],[52.519635,13.376831],[52.50611111111111,13.385555555555555],[52.51627222222222,13.377722222222223],[52.51638888888889,13.380833333333333],[52.504444444444445,13.383055555555556],[52.51,13.373611111111112]],[[52.45616,13.2919],[52.45721,13.2895],[52.4557,13.29446],[52.45647,13.29295],[52.45836,13.28861],[52.457,13.292],[52.45666666666667,13.291944444444445],[52.4575,13.289722222222222],[52.45583333333334,13.2925],[52.4581,13.28702],[52.4558333333,13.2927777778]]]
         */
        // area around betahaus
        val polygonPoints = arrayOf(
            doubleArrayOf(52.502286, 13.412372),
            doubleArrayOf(52.49906, 13.418273),
            doubleArrayOf(52.503618, 13.410741),
            doubleArrayOf(52.498589, 13.404884),
            doubleArrayOf(52.506178, 13.403854),
            doubleArrayOf(52.502743, 13.424474),
            doubleArrayOf(52.498354, 13.413166),
            doubleArrayOf(52.502913, 13.407909),
            doubleArrayOf(52.50011, 13.30723),
            doubleArrayOf(52.49632, 13.32024),
            doubleArrayOf(52.4957, 13.3033)
        )
        // ,{52.49939,13.30523},{52.5004,13.312},{52.50011,13.30723},{52.49907,13.31225},{52.50019,13.30983},{52.49984,13.30646},{52.4968,13.3053},{52.5002,13.3103},{52.50083333333333,13.312777777777779},{52.5,13.307222222222222}};
        val polygon = polygonForPoints(polygonPoints)
        //        double[][] polygon = ConvexHull.cvxHull(polygonPoints);
        val buf = StringBuilder()
        buf.append("[")
        for (p in polygon) {
            buf.append("[" + p[0] + "," + p[1] + "],")
        }

        buf.deleteAt(buf.length - 1)
        buf.append("]")

        // FIXME complete test
    }

    @Test
    fun polygonForPointsInFourQuadrantsShouldContainStuffInside() {
        val polygon =
            polygonForPoints(arrayOf(sydney, newyork, amsterdam, buenosaires))
        polygonContains(london[1], london[0], *polygon) shouldBe true
        polygonContains(berlin[1], berlin[0], *polygon) shouldBe false
    }

    @Test
    fun shouldConvertCircleToPolygonOn180() {
        val circle2polygon = circle2polygon(6, -18.0, 180.0, 1000.0)
        var countEast = 0
        for (point in circle2polygon[0]) {
            val distance = distance(-18.0, 180.0, point[1], point[0])
            abs(1000 - distance) shouldBeLessThan 1.0
            if (isWest(180.0, point[0])) {
                countEast++
            }
        }
        countEast shouldBeGreaterThan 1
    }

    @Test
    fun shouldConvertCircleToPolygonOnNorthPole() {
        val lat = 89.9
        val lon = 0.0
        val circle2polygon = circle2polygon(6, lat, lon, 1000.0)
        var countEast = 0
        for (point in circle2polygon[0]) {
            val distance = distance(lat, lon, point[1], point[0])
            abs(1000 - distance) shouldBeLessThan 20.0
            if (isWest(180.0, point[0])) {
                countEast++
            }
        }
        countEast shouldBeGreaterThan 1
    }

    @Test
    fun checkIntersectionIssue12() {
        // reported by a user
        // line 1 is vertical
        // line 2 intersects in between the points of line 1
        // however, the intersection point is outside of the points of line2 (this was the bug)
        val l1p1 = doubleArrayOf(-71.1884310511, 42.3219864254)
        val l1p2 = doubleArrayOf(-71.1884310511, 42.321998793)
        val l2p1 = doubleArrayOf(-71.1884310515, 42.3221529806)
        val l2p2 = doubleArrayOf(-71.1884310517, 42.3222331303)

        GeoGeometry.linesCross(l1p1, l1p2, l2p1, l2p2) shouldBe false
        GeoGeometry.linesCross(l2p1, l2p2, l1p1, l1p2) shouldBe false
    }

    @Test
    fun shouldFilterPoints() {
        val latitude = 52.0
        val longitude = 13.0
        val points = Array(1000) { DoubleArray(2) }

        for (i in 0..999) {
            points[i] = doubleArrayOf(longitude + Random.nextDouble(), latitude + Random.nextDouble())
        }
        // insert a few 'bad' points
        points[50] = doubleArrayOf(100.0, 100.0)
        points[100] = doubleArrayOf(-100.0, 100.0)
        points[150] = doubleArrayOf(100.0, -100.0)
        points[200] = doubleArrayOf(-100.0, -100.0)
        val filtered = filterNoiseFromPointCloud(points, 0.005f)
        filtered.size shouldBe 996
        val bbox = boundingBox(filtered)

        // the four bad pois should be gone
        bbox[1] shouldBeGreaterThan 52.0
        bbox[1] shouldBeLessThan 53.0

        bbox[3] shouldBeGreaterThan 52.0
        bbox[3] shouldBeLessThan 53.0

        bbox[0] shouldBeGreaterThan 13.0
        bbox[0] shouldBeLessThan 14.0

        bbox[2] shouldBeGreaterThan 13.0
        bbox[2] shouldBeLessThan 14.0
    }

    @Test
    fun shouldOverLapWithSelf() {
        overlap(samplePolygon, samplePolygon) shouldBe true
    }

    val overlappingPolygons = listOf(
        arrayOf(
            berlin,
            amsterdam,
            newyork
        ) to arrayOf(london, potsDammerPlatz, moritzPlatz),

        arrayOf(rosenthalerPlatz, moritzPlatz, brandenBurgerGate) to
            arrayOf(oranienburgerTor, potsDammerPlatz, senefelderPlatz)

    )

    @Test
    fun shouldOverlap() {
        overlappingPolygons.forEach { (left, right) ->
            overlap(left,right) shouldBe true
            overlap(right,left) shouldBe true
        }
    }

    @Test
    fun shouldExpandPolygon() {
        val expandPolygon = expandPolygon(2000, samplePolygon)
        contains(expandPolygon,samplePolygon) shouldBe true
    }

    @Test
    fun shouldBeRight() {
        withClue("should be right") {
            rightTurn(doubleArrayOf(1.0, 1.0), doubleArrayOf(2.0, 2.0), doubleArrayOf(1.0, 10.0)) shouldBe true
        }
        withClue("should be right") {
            rightTurn(doubleArrayOf(1.0, 1.0), doubleArrayOf(2.0, 2.0), doubleArrayOf(3.0, 4.0)) shouldBe true
        }
        withClue("should not be right") {
            rightTurn(doubleArrayOf(1.0, 1.0), doubleArrayOf(0.0, 2.0), doubleArrayOf(1.0, 10.0)) shouldBe false
        }
        withClue("should not be right") {
            rightTurn(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 10.0)) shouldBe false
        }
    }

    val clouds =
        listOf(
            arrayOf(
                doubleArrayOf(1.0, 1.0),
                doubleArrayOf(2.0, 2.0),
                doubleArrayOf(3.0, 3.0),
                doubleArrayOf(3.0, 0.0),
                doubleArrayOf(0.0, 3.0)
            ) to arrayOf(doubleArrayOf(2.0, 2.0)),
            arrayOf(
                doubleArrayOf(1.0, 1.0),
                doubleArrayOf(-1.0, -1.0),
                doubleArrayOf(-1.0, 1.0),
                doubleArrayOf(1.0, -1.0),
                doubleArrayOf(0.0, 0.0)
            ) to arrayOf(doubleArrayOf(0.0, 0.0))

        )

    @Test
    fun shouldCalculatePolygonForPointCloud() {
        clouds.forEach { (points, contained) ->
            val polygonForPoints = polygonForPoints(points)
            for (i in contained.indices) {
                polygonContains(
                    contained[i][1],
                    contained[i][0],
                    *polygonForPoints
                ) shouldBe true
            }
        }
    }

    @Test
    fun shouldCalculateCorrectBbox() {
        val bbox = bbox(berlin[1], berlin[0], 1000.0, 1000.0)
        distance(
            bbox[1],
            berlin[0],
            berlin[1],
            berlin[0]
        ) shouldBeGreaterThan 499.0
        distance(
            bbox[1],
            berlin[0],
            berlin[1],
            berlin[0]
        ) shouldBeLessThan 501.0
        distance(
            bbox[3],
            berlin[0],
            berlin[1],
            berlin[0]
        ) shouldBeGreaterThan 499.0
        distance(
            bbox[3],
            berlin[0],
            berlin[1],
            berlin[0]
        ) shouldBeLessThan 501.0
        distance(
            berlin[1],
            bbox[0],
            berlin[1],
            berlin[0]
        ) shouldBeGreaterThan 499.0
        distance(
            berlin[1],
            bbox[0],
            berlin[1],
            berlin[0]
        ) shouldBeLessThan 501.0
        distance(
            berlin[1],
            bbox[2],
            berlin[1],
            berlin[0]
        ) shouldBeGreaterThan 499.0
        distance(
            berlin[1],
            bbox[2],
            berlin[1],
            berlin[0]
        ) shouldBeLessThan 501.0
    }

    @Test
    fun shouldCalculateBboxForPoint() {
        val bbox = boundingBox(doubleArrayOf(13.0, 52.0))
        bbox[1] shouldBe 52.0
        bbox[3] shouldBe 52.0
        bbox[0] shouldBe 13.0
        bbox[2] shouldBe 13.0
    }

    @Test
    fun shouldCalculateBboxForLinePolygonMultiPolygon() {
        val line =
            circle2polygon(5000, 52.0, 13.0, 1000.0)[0]
        val polygon = arrayOf(line)
        val multiPolygon =
            arrayOf(polygon)

        area(
            boundingBox(
                line
            )
        ) shouldBe area(
            boundingBox(polygon)
        )

        area(
            boundingBox(
                line
            )
        ) shouldBe area(
            boundingBox(multiPolygon)
        )
    }

    val linesNPoints = listOf(
        doubleArrayOf(
            52.520316,
            13.414654,
            52.528149,
            13.423709,
            52.52392,
            13.412122,
            416.0
        ),
        doubleArrayOf(52.521337, 13.403108, 52.517002, 13.409073, 52.519039, 13.408665, 138.0),
        doubleArrayOf(52.471551, 13.385791, 52.478139, 13.385791, 52.476244, 13.384718, 73.0),
        doubleArrayOf(52.476244, 13.384718, 52.476244, 14.0, 52.478139, 13.385062, 211.0),
        doubleArrayOf(1.0, 1.0, 3.0, 3.0, 2.1, 2.1, 0.0),
        doubleArrayOf(1.0, 1.0, 3.0, 1.0, 2.0, 1.0, 0.0),
        doubleArrayOf(1.0, 1.0, 1.0, 3.0, 1.0, 2.0, 0.0),
        doubleArrayOf(1.0, 1.0, 3.0, 3.0, 2.0, 2.1, 7860.0),
        doubleArrayOf(1.0, 1.0, 3.0, 3.0, 90.0, 90.0, round(distance(3.0, 3.0, 90.0, 90.0))),
        doubleArrayOf(1.0, 1.0, 3.0, 3.0, 0.0, 0.0, round(distance(1.0, 1.0, 0.0, 0.0)))
    )

    // Ugly ;-)
    private operator fun DoubleArray.component6() = this[5]
    private operator fun DoubleArray.component7() = this[6]

    @Test
    fun shouldCalculateDistanceToLine() {
        linesNPoints.forEach { (x1, y1, x2, y2, px, py, expectedDistance) ->

            val distance = distance(x1, y1, x2, y2, px, py)
            val distance2 =
                distance(doubleArrayOf(y1, x1), doubleArrayOf(y2, x2), doubleArrayOf(py, px))
            distance shouldBe distance2
            round(distance) shouldBe expectedDistance

            val distToPoint1 = distance(x1, y1, px, py)
            val distToPoint2 = distance(x1, y1, px, py)
            distance shouldBeLessThanOrEqual distToPoint1
            distance shouldBeLessThanOrEqual distToPoint2
        }
    }

    @Test
    fun shouldCalculateDistanceToLineString() {
        val ls = arrayOf(
            doubleArrayOf(13.414654, 52.520316),
            doubleArrayOf(13.423709, 52.528149),
            doubleArrayOf(13.425724, 52.524992)
        )
        var d = round(distanceToLineString(doubleArrayOf(13.412122, 52.52392), ls))
        d shouldBe 416
        val ls2 = arrayOf(
            doubleArrayOf(13.425724, 52.524992),
            doubleArrayOf(13.414654, 52.520316),
            doubleArrayOf(13.423709, 52.528149)
        )
        d = round(distanceToLineString(doubleArrayOf(13.412122, 52.52392), ls2))
        d shouldBe 416
    }

    @Test
    fun shouldCalculateDistanceToPolygon() {
        val polygon = arrayOf(
            doubleArrayOf(13.414654, 52.520316),
            doubleArrayOf(13.423709, 52.528149),
            doubleArrayOf(13.425724, 52.524992),
            doubleArrayOf(13.414654, 52.520316)
        )
        var d = round(distanceToPolygon(doubleArrayOf(13.412122, 52.52392), polygon))
        d shouldBe 416
        val polygon2 = arrayOf(
            doubleArrayOf(13.425724, 52.524992),
            doubleArrayOf(13.414654, 52.520316),
            doubleArrayOf(13.423709, 52.528149),
            doubleArrayOf(13.425724, 52.524992)
        )
        d = round(distanceToPolygon(doubleArrayOf(13.412122, 52.52392), polygon2))
        d shouldBe 416
        d = round(
            distanceToPolygon(
                doubleArrayOf(13.412122, 52.52392),
                arrayOf(polygon2)
            )
        )
        d shouldBe 416
        val center = polygonCenter(*polygon)
        d = round(distanceToPolygon(center, polygon2))
        d shouldBe 0
    }

    @Test
    fun shouldCalculateDistanceToMultiPolygon() {
        val polygon = arrayOf(
            doubleArrayOf(13.414654, 52.520316),
            doubleArrayOf(13.423709, 52.528149),
            doubleArrayOf(13.425724, 52.524992),
            doubleArrayOf(13.414654, 52.520316)
        )
        val multiPolygon =
            arrayOf(
                arrayOf(polygon),
                arrayOf(
                    arrayOf(
                        doubleArrayOf(1.0, 1.0),
                        doubleArrayOf(1.0, 2.0),
                        doubleArrayOf(2.0, 2.0),
                        doubleArrayOf(1.0, 1.0)
                    )
                )
            )
        val multiPolygon2 =
            arrayOf(
                arrayOf(
                    arrayOf(
                        doubleArrayOf(1.0, 1.0),
                        doubleArrayOf(1.0, 2.0),
                        doubleArrayOf(2.0, 2.0),
                        doubleArrayOf(1.0, 1.0)
                    )
                ),
                arrayOf(polygon)
            )
        var d =
            round(distanceToMultiPolygon(doubleArrayOf(13.412122, 52.52392), multiPolygon))
        d shouldBe 416
        d = round(distanceToMultiPolygon(doubleArrayOf(13.412122, 52.52392), multiPolygon2))
        d shouldBe 416
    }

    @Test
    fun shouldCalculateArea() {
        val circle =
            circle2polygon(5000, 52.0, 13.0, 1000.0)
        val calculatedArea = area(circle)
        val circleArea = PI * 1000 * 1000
        // 0.005% difference allowed perfect circle area and calculated area
        abs(circleArea - calculatedArea) shouldBeLessThan calculatedArea / 200
    }

    @Test
    fun shouldCalculateAreaForBbox() {
        val radius = 1000
        val polygonCoordinates =
            circle2polygon(15, 52.0, 13.0, radius.toDouble())
        val bboxForCircle = boundingBox(polygonCoordinates)
        val area = area(bboxForCircle)
        val expectedArea = radius * 2 * radius * 2.toDouble()
        //        System.out.println(area);
//        System.out.println(expectedArea);
//
//        Feature f = new Feature(new Polygon(polygonCoordinates, null), null, null);
//
//        List<Feature> l = new ArrayList<Feature>();
//        l.add(f);
//        l.add(new Feature(new Point(new double[]{bboxForCircle[0],bboxForCircle[1]},null),null,null));
//        l.add(new Feature(new Point(new double[]{bboxForCircle[2],bboxForCircle[3]},null),null,null));
//        System.out.println(gson.toJson(new FeatureCollection(l,null)));

        // area should be roughly similar to that of the ideal rectangle of 2000x2000
        abs(area - expectedArea) shouldBeLessThan expectedArea * 0.05
    }

    @Test
    fun shouldCalculateAreaOfPolygonWithHole() {
        val outer = circle2polygon(5000, 52.0, 13.0, 1000.0)[0]
        val inner = circle2polygon(5000, 52.0, 13.0, 1000.0)[0]
        val polygon =
            arrayOf(outer, inner)
        area(polygon) shouldBe area(outer) - area(inner)
    }

    @Test
    fun shouldCalculateAreaOfMultiPolygon() {
        val outer = circle2polygon(5000, 52.0, 13.0, 1000.0)[0]
        val inner = circle2polygon(5000, 52.0, 13.0, 1000.0)[0]
        val polygon =
            arrayOf(outer, inner)
        val multiPolygon =
            arrayOf(polygon, polygon)

        area(multiPolygon) shouldBe 2* area(polygon)
    }

    val straightLines = arrayOf(
        arrayOf(
            doubleArrayOf(
                0.0,
                1.0
            ),
            doubleArrayOf(1.0, 1.0),
            doubleArrayOf(2.0, 1.0),
            doubleArrayOf(3.0, 1.0),
            doubleArrayOf(4.0, 1.0)
        ),
        arrayOf(
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(1.0, 1.0),
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(1.0, 3.0),
            doubleArrayOf(1.0, 4.0)
        ),
        arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 1.0),
            doubleArrayOf(2.0, 2.0),
            doubleArrayOf(3.0, 3.0),
            doubleArrayOf(4.0, 4.0)
        ),
        arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 1.0),
            doubleArrayOf(2.0, 2.0),
            doubleArrayOf(4.0, 4.0)
        ),
        arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 1.0),
            doubleArrayOf(4.0, 4.0)
        ),
        arrayOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 1.0))
    )

    @Test
    fun shouldSimplifyStraightLines() {
        straightLines.forEach { line ->
            val simplified = simplifyLine(line, 1.0)
            simplified.size shouldBe 2
        }
    }

    @Test
    fun shouldSimplifyCircle() {
        val poly =
            circle2polygon(10000, 52.0, 13.0, 1000.0)[0] // 10000 segments!
        val simplified = simplifyLine(poly, 100.0)
        simplified shouldHaveAtMostSize poly.size
    }

    @Test
    fun shouldValidate() {
        arrayOf(
            amsterdam, berlin, doubleArrayOf(180.0000004999, 52.0), doubleArrayOf(180.0000004999, 90.0000004999)
        ).forEach { point ->
            // should throw no exceptions
            validate(point)
        }
    }

    @Test
    fun shouldNotValidate() {
        assertSoftly {
            arrayOf(
                doubleArrayOf(180.001, 0.0),
                doubleArrayOf(-180.001, 0.0),
                doubleArrayOf(0.0, 90.001),
                doubleArrayOf(0.0, -90.001)
            ).forEach {
                shouldThrow<IllegalArgumentException> {
                    validate(it)
                }
            }
        }
    }

    @Test
    fun issue5PolygonContainsDoesNotWorkCorrectly() {
        val polygon = arrayOf(
            arrayOf(
                doubleArrayOf(
                    42.503320312499994,
                    1.7060546875
                ),
                doubleArrayOf(42.4966796875, 1.678515625000017),
                doubleArrayOf(42.455957031249994, 1.58642578125),
                doubleArrayOf(42.441699218749996, 1.534082031250023),
                doubleArrayOf(42.434472656249994, 1.486230468750023),
                doubleArrayOf(42.437451171875, 1.448828125),
                doubleArrayOf(42.46132812499999, 1.428125),
                doubleArrayOf(42.497851562499996, 1.430273437500006),
                doubleArrayOf(42.530810546874996, 1.421972656250006),
                doubleArrayOf(42.548388671874996, 1.414843750000017),
                doubleArrayOf(42.5958984375, 1.428320312500006),
                doubleArrayOf(42.6216796875, 1.458886718750023),
                doubleArrayOf(42.642724609374994, 1.501367187500023),
                doubleArrayOf(42.635009765625, 1.568164062500017),
                doubleArrayOf(42.604443359375, 1.709863281250023),
                doubleArrayOf(42.575927734375, 1.739453125000011),
                doubleArrayOf(42.55673828125, 1.740234375),
                doubleArrayOf(42.525634765625, 1.713964843750006),
                doubleArrayOf(42.503320312499994, 1.7060546875)
            )
        )
        val point = doubleArrayOf(42.503615, 1.641881)
        polygonContains(point,polygon) shouldBe true
    }
}
