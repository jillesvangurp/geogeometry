package com.jillesvangurp.geogeometry
import com.jillesvangurp.geojson.lonLat
import com.jillesvangurp.geojson.linearRingCoordinates

val bergstr16Berlin = lonLat(13.3941763, 52.5298311)
val brandenBurgerGate = lonLat(13.377157, 52.516279)
val potsDammerPlatz = lonLat(13.376599, 52.509515)
val moritzPlatz = lonLat(13.410717, 52.503663)
val senefelderPlatz = lonLat(13.412949, 52.532755)
val naturkundeMuseum = lonLat(13.381921, 52.531188)
val rosenthalerPlatz = lonLat(13.401361, 52.529948)
val oranienburgerTor = lonLat(13.38707, 52.525339)

val bigRing = linearRingCoordinates(potsDammerPlatz, brandenBurgerGate, naturkundeMuseum, senefelderPlatz, moritzPlatz)
val smallRing = linearRingCoordinates(rosenthalerPlatz, oranienburgerTor, bergstr16Berlin)

val concavePoly = """
{
        "type": "Polygon",
        "coordinates": [
          [
            [
              13.391647338867186,
              52.53648187013421
            ],
            [
              13.377828598022461,
              52.53277501350654
            ],
            [
              13.372678756713867,
              52.52478593472457
            ],
            [
              13.371133804321287,
              52.51554185396127
            ],
            [
              13.379030227661131,
              52.50734067419732
            ],
            [
              13.392162322998047,
              52.50258642348695
            ],
            [
              13.408899307250977,
              52.50368360390624
            ],
            [
              13.407783508300781,
              52.50697498086002
            ],
            [
              13.39353561401367,
              52.50634807091736
            ],
            [
              13.383321762084961,
              52.50974372615093
            ],
            [
              13.377485275268555,
              52.51836228817983
            ],
            [
              13.381433486938477,
              52.52640475435762
            ],
            [
              13.389673233032225,
              52.53277501350654
            ],
            [
              13.407440185546875,
              52.53355817830341
            ],
            [
              13.423919677734375,
              52.53543771682821
            ],
            [
              13.421430587768555,
              52.53950977430533
            ],
            [
              13.391647338867186,
              52.53648187013421
            ]
          ]
        ]
      }      
""".trimIndent()

val testPolygon = """
        {
        "coordinates": [
          [
            [
              13.431665897369385,
              52.52666584871098
            ],
            [
              13.431397676467894,
              52.52619587775743
            ],
            [
              13.432052135467528,
              52.52590214335785
            ],
            [
              13.432599306106567,
              52.52608491165957
            ],
            [
              13.432663679122925,
              52.52616324069894
            ],
            [
              13.432663679122925,
              52.526450445981496
            ],
            [
              13.432331085205078,
              52.52658099321639
            ],
            [
              13.431966304779053,
              52.52665279403019
            ],
            [
              13.431665897369385,
              52.52666584871098
            ]
          ]
        ],
        "type": "Polygon"
      }
    """.trimIndent()

val badGeo = """
{
              "coordinates": [
                [
                  [
                    7.6022035414,
                    51.9755909366,
                    0
                  ],
                  [
                    7.6022042237,
                    51.9755914897,
                    0
                  ],
                  [
                    7.6022425639,
                    51.9756165784,
                    0
                  ],
                  [
                    7.6022436804,
                    51.9756171699,
                    0
                  ],
                  [
                    7.6022466341,
                    51.9756184175,
                    0
                  ],
                  [
                    7.6022498449,
                    51.9756205841,
                    0
                  ],
                  [
                    7.6022513603,
                    51.9756221062,
                    0
                  ],
                  [
                    7.6022527814,
                    51.975623258,
                    0
                  ],
                  [
                    7.6022796382,
                    51.9756408312,
                    0
                  ],
                  [
                    7.6022804291,
                    51.9756412502,
                    0
                  ],
                  [
                    7.6022812301,
                    51.975641197,
                    0
                  ],
                  [
                    7.6022841497,
                    51.9756412167,
                    0
                  ],
                  [
                    7.6022855423,
                    51.9756413281,
                    0
                  ],
                  [
                    7.6022866961,
                    51.9756409456,
                    0
                  ],
                  [
                    7.6022881173,
                    51.975640312,
                    0
                  ],
                  [
                    7.6022920408,
                    51.9756394425,
                    0
                  ],
                  [
                    7.602296059,
                    51.9756393749,
                    0
                  ],
                  [
                    7.6023000095,
                    51.975640112,
                    0
                  ],
                  [
                    7.6023037329,
                    51.975641624,
                    0
                  ],
                  [
                    7.6023070789,
                    51.9756438499,
                    0
                  ],
                  [
                    7.6023099124,
                    51.9756466998,
                    0
                  ],
                  [
                    7.6023121189,
                    51.9756500586,
                    0
                  ],
                  [
                    7.6023136093,
                    51.9756537907,
                    0
                  ],
                  [
                    7.6023143236,
                    51.9756577454,
                    0
                  ],
                  [
                    7.6023142328,
                    51.9756617631,
                    0
                  ],
                  [
                    7.6023133406,
                    51.9756656815,
                    0
                  ],
                  [
                    7.6023127855,
                    51.9756673318,
                    0
                  ],
                  [
                    7.6023134576,
                    51.9756673808,
                    0
                  ],
                  [
                    7.6023151658,
                    51.9756673215,
                    0
                  ],
                  [
                    7.6023186361,
                    51.9756678133,
                    0
                  ],
                  [
                    7.6023219673,
                    51.9756689033,
                    0
                  ],
                  [
                    7.602325057,
                    51.9756705582,
                    0
                  ],
                  [
                    7.6023444764,
                    51.9756832661,
                    0
                  ],
                  [
                    7.6023451656,
                    51.9756836313,
                    0
                  ],
                  [
                    7.6023483967,
                    51.9756857675,
                    0
                  ],
                  [
                    7.6023487993,
                    51.9756860939,
                    0
                  ],
                  [
                    7.6023838435,
                    51.975709026,
                    0
                  ],
                  [
                    7.6023851405,
                    51.9757097131,
                    0
                  ],
                  [
                    7.6023883713,
                    51.9757118491,
                    0
                  ],
                  [
                    7.602389265,
                    51.9757125735,
                    0
                  ],
                  [
                    7.602437698,
                    51.9757442651,
                    0
                  ],
                  [
                    7.6024384731,
                    51.9757446756,
                    0
                  ],
                  [
                    7.6024392683,
                    51.9757443057,
                    0
                  ],
                  [
                    7.6024690322,
                    51.9757269574,
                    0
                  ],
                  [
                    7.6024693927,
                    51.9757266948,
                    0
                  ],
                  [
                    7.6024727303,
                    51.9757247283,
                    0
                  ],
                  [
                    7.602473355,
                    51.9757244377,
                    0
                  ],
                  [
                    7.6025185777,
                    51.9756980799,
                    0
                  ],
                  [
                    7.6025192877,
                    51.9756975628,
                    0
                  ],
                  [
                    7.6025186054,
                    51.9756970097,
                    0
                  ],
                  [
                    7.6025141638,
                    51.9756941034,
                    0
                  ],
                  [
                    7.6025110083,
                    51.9756915456,
                    0
                  ],
                  [
                    7.6025084347,
                    51.9756884031,
                    0
                  ],
                  [
                    7.6025065491,
                    51.9756848054,
                    0
                  ],
                  [
                    7.6025054293,
                    51.9756809009,
                    0
                  ],
                  [
                    7.6025051214,
                    51.9756768507,
                    0
                  ],
                  [
                    7.6025056383,
                    51.9756728219,
                    0
                  ],
                  [
                    7.6025069584,
                    51.9756689805,
                    0
                  ],
                  [
                    7.6025090275,
                    51.9756654851,
                    0
                  ],
                  [
                    7.6025117601,
                    51.9756624798,
                    0
                  ],
                  [
                    7.6025150436,
                    51.9756600886,
                    0
                  ],
                  [
                    7.6025536429,
                    51.9756375912,
                    0
                  ],
                  [
                    7.6025546197,
                    51.9756368799,
                    0
                  ],
                  [
                    7.6025579568,
                    51.9756349136,
                    0
                  ],
                  [
                    7.6025593432,
                    51.9756342686,
                    0
                  ],
                  [
                    7.6026159972,
                    51.9756012469,
                    0
                  ],
                  [
                    7.6026169733,
                    51.975600536,
                    0
                  ],
                  [
                    7.6026203109,
                    51.9755985695,
                    0
                  ],
                  [
                    7.6026216975,
                    51.9755979244,
                    0
                  ],
                  [
                    7.6026783528,
                    51.9755649027,
                    0
                  ],
                  [
                    7.6026793318,
                    51.9755641897,
                    0
                  ],
                  [
                    7.6026826692,
                    51.9755622233,
                    0
                  ],
                  [
                    7.6026840516,
                    51.9755615802,
                    0
                  ],
                  [
                    7.6027400117,
                    51.9755289637,
                    0
                  ],
                  [
                    7.602743353,
                    51.9755274092,
                    0
                  ],
                  [
                    7.6027469228,
                    51.9755264941,
                    0
                  ],
                  [
                    7.6027506,
                    51.9755262496,
                    0
                  ],
                  [
                    7.6027542595,
                    51.9755266838,
                    0
                  ],
                  [
                    7.6027577773,
                    51.9755277822,
                    0
                  ],
                  [
                    7.6027610338,
                    51.9755295074,
                    0
                  ],
                  [
                    7.602782291,
                    51.9755434171,
                    0
                  ],
                  [
                    7.6027830661,
                    51.9755438276,
                    0
                  ],
                  [
                    7.6027838613,
                    51.9755434577,
                    0
                  ],
                  [
                    7.602813097,
                    51.9755264169,
                    0
                  ],
                  [
                    7.6028134576,
                    51.9755261543,
                    0
                  ],
                  [
                    7.6028167951,
                    51.9755241877,
                    0
                  ],
                  [
                    7.6028174197,
                    51.9755238971,
                    0
                  ],
                  [
                    7.6028631699,
                    51.9754972307,
                    0
                  ],
                  [
                    7.6028638799,
                    51.9754967136,
                    0
                  ],
                  [
                    7.6028631976,
                    51.9754961605,
                    0
                  ],
                  [
                    7.6028240861,
                    51.9754705671,
                    0
                  ],
                  [
                    7.6028209307,
                    51.9754680094,
                    0
                  ],
                  [
                    7.6028183571,
                    51.9754648669,
                    0
                  ],
                  [
                    7.6028164716,
                    51.9754612692,
                    0
                  ],
                  [
                    7.6028153518,
                    51.9754573647,
                    0
                  ],
                  [
                    7.602815044,
                    51.9754533145,
                    0
                  ],
                  [
                    7.6028155608,
                    51.9754492857,
                    0
                  ],
                  [
                    7.602816881,
                    51.9754454443,
                    0
                  ],
                  [
                    7.60281895,
                    51.975441949,
                    0
                  ],
                  [
                    7.6028216826,
                    51.9754389437,
                    0
                  ],
                  [
                    7.6028249661,
                    51.9754365525,
                    0
                  ],
                  [
                    7.6028430793,
                    51.9754259953,
                    0
                  ],
                  [
                    7.6028437893,
                    51.9754254782,
                    0
                  ],
                  [
                    7.602843107,
                    51.9754249251,
                    0
                  ],
                  [
                    7.6028219509,
                    51.9754110812,
                    0
                  ],
                  [
                    7.6028211758,
                    51.9754106707,
                    0
                  ],
                  [
                    7.6028203806,
                    51.9754110406,
                    0
                  ],
                  [
                    7.6027987668,
                    51.9754236382,
                    0
                  ],
                  [
                    7.6027979653,
                    51.9754242219,
                    0
                  ],
                  [
                    7.6027977704,
                    51.9754245511,
                    0
                  ],
                  [
                    7.6027964279,
                    51.9754274623,
                    0
                  ],
                  [
                    7.6027940364,
                    51.9754307133,
                    0
                  ],
                  [
                    7.602791041,
                    51.975433418,
                    0
                  ],
                  [
                    7.6027875636,
                    51.9754354662,
                    0
                  ],
                  [
                    7.6027837458,
                    51.9754367746,
                    0
                  ],
                  [
                    7.602779743,
                    51.9754372899,
                    0
                  ],
                  [
                    7.6027757183,
                    51.9754369911,
                    0
                  ],
                  [
                    7.6027718355,
                    51.9754358904,
                    0
                  ],
                  [
                    7.6027682527,
                    51.9754340325,
                    0
                  ],
                  [
                    7.6027541251,
                    51.9754247879,
                    0
                  ],
                  [
                    7.6027513348,
                    51.9754225843,
                    0
                  ],
                  [
                    7.6027489788,
                    51.9754199215,
                    0
                  ],
                  [
                    7.6027471315,
                    51.9754168835,
                    0
                  ],
                  [
                    7.602746612,
                    51.9754155374,
                    0
                  ],
                  [
                    7.6027461953,
                    51.9754147424,
                    0
                  ],
                  [
                    7.6027454251,
                    51.975414118,
                    0
                  ],
                  [
                    7.6027170746,
                    51.975395567,
                    0
                  ],
                  [
                    7.6027162835,
                    51.975395148,
                    0
                  ],
                  [
                    7.6027161945,
                    51.9753951539,
                    0
                  ],
                  [
                    7.602711901,
                    51.9753949768,
                    0
                  ],
                  [
                    7.6027077443,
                    51.9753938866,
                    0
                  ],
                  [
                    7.6027039165,
                    51.9753919336,
                    0
                  ],
                  [
                    7.6026734863,
                    51.9753720221,
                    0
                  ],
                  [
                    7.6026721898,
                    51.9753713354,
                    0
                  ],
                  [
                    7.6026689582,
                    51.9753691988,
                    0
                  ],
                  [
                    7.6026680648,
                    51.9753684746,
                    0
                  ],
                  [
                    7.6026297231,
                    51.9753433851,
                    0
                  ],
                  [
                    7.602628948,
                    51.9753429745,
                    0
                  ],
                  [
                    7.6026281528,
                    51.9753433444,
                    0
                  ],
                  [
                    7.6025488442,
                    51.9753895712,
                    0
                  ],
                  [
                    7.6025481342,
                    51.9753900883,
                    0
                  ],
                  [
                    7.6025488165,
                    51.9753906414,
                    0
                  ],
                  [
                    7.6025833786,
                    51.9754132579,
                    0
                  ],
                  [
                    7.602586389,
                    51.9754156728,
                    0
                  ],
                  [
                    7.6025888795,
                    51.9754186209,
                    0
                  ],
                  [
                    7.6025907574,
                    51.9754219925,
                    0
                  ],
                  [
                    7.6025919528,
                    51.975425662,
                    0
                  ],
                  [
                    7.6025924212,
                    51.9754294928,
                    0
                  ],
                  [
                    7.6025921451,
                    51.9754333422,
                    0
                  ],
                  [
                    7.6025911348,
                    51.9754370669,
                    0
                  ],
                  [
                    7.6025894279,
                    51.9754405282,
                    0
                  ],
                  [
                    7.602587088,
                    51.9754435972,
                    0
                  ],
                  [
                    7.6025842022,
                    51.9754461597,
                    0
                  ],
                  [
                    7.6025839087,
                    51.9754463735,
                    0
                  ],
                  [
                    7.602584591,
                    51.9754469265,
                    0
                  ],
                  [
                    7.6026016504,
                    51.9754580891,
                    0
                  ],
                  [
                    7.6026030306,
                    51.9754588202,
                    0
                  ],
                  [
                    7.6026054311,
                    51.9754598321,
                    0
                  ],
                  [
                    7.6026086237,
                    51.9754619768,
                    0
                  ],
                  [
                    7.6026113468,
                    51.975464693,
                    0
                  ],
                  [
                    7.6026134995,
                    51.9754678802,
                    0
                  ],
                  [
                    7.6026150024,
                    51.9754714205,
                    0
                  ],
                  [
                    7.6026157999,
                    51.9754751831,
                    0
                  ],
                  [
                    7.6026158624,
                    51.9754790287,
                    0
                  ],
                  [
                    7.6026151876,
                    51.9754828151,
                    0
                  ],
                  [
                    7.6026138005,
                    51.9754864024,
                    0
                  ],
                  [
                    7.6026117524,
                    51.9754896578,
                    0
                  ],
                  [
                    7.602609119,
                    51.9754924611,
                    0
                  ],
                  [
                    7.6026059978,
                    51.9754947084,
                    0
                  ],
                  [
                    7.6025933121,
                    51.9755021024,
                    0
                  ],
                  [
                    7.6025923355,
                    51.9755028136,
                    0
                  ],
                  [
                    7.602588998,
                    51.9755047801,
                    0
                  ],
                  [
                    7.6025876118,
                    51.975505425,
                    0
                  ],
                  [
                    7.6025309565,
                    51.9755384466,
                    0
                  ],
                  [
                    7.6025299802,
                    51.9755391576,
                    0
                  ],
                  [
                    7.602526643,
                    51.9755411239,
                    0
                  ],
                  [
                    7.6025252562,
                    51.9755417691,
                    0
                  ],
                  [
                    7.6024686023,
                    51.9755747908,
                    0
                  ],
                  [
                    7.6024676296,
                    51.9755754992,
                    0
                  ],
                  [
                    7.6024642919,
                    51.9755774658,
                    0
                  ],
                  [
                    7.602462902,
                    51.9755781124,
                    0
                  ],
                  [
                    7.6024418844,
                    51.9755903625,
                    0
                  ],
                  [
                    7.6024383169,
                    51.9755919963,
                    0
                  ],
                  [
                    7.6024344991,
                    51.9755929021,
                    0
                  ],
                  [
                    7.6024305779,
                    51.9755930451,
                    0
                  ],
                  [
                    7.6024267043,
                    51.9755924197,
                    0
                  ],
                  [
                    7.6024230273,
                    51.9755910501,
                    0
                  ],
                  [
                    7.6024196885,
                    51.975588989,
                    0
                  ],
                  [
                    7.6024168164,
                    51.9755863156,
                    0
                  ],
                  [
                    7.6024145215,
                    51.9755831329,
                    0
                  ],
                  [
                    7.6024128921,
                    51.9755795634,
                    0
                  ],
                  [
                    7.6024119911,
                    51.9755757445,
                    0
                  ],
                  [
                    7.602411853,
                    51.9755718232,
                    0
                  ],
                  [
                    7.6024119959,
                    51.9755709447,
                    0
                  ],
                  [
                    7.6024120589,
                    51.975569856,
                    0
                  ],
                  [
                    7.6024097016,
                    51.9755729731,
                    0
                  ],
                  [
                    7.6024067017,
                    51.9755756198,
                    0
                  ],
                  [
                    7.6024032351,
                    51.9755776165,
                    0
                  ],
                  [
                    7.6023994404,
                    51.9755788833,
                    0
                  ],
                  [
                    7.6023954696,
                    51.9755793696,
                    0
                  ],
                  [
                    7.6023914814,
                    51.9755790558,
                    0
                  ],
                  [
                    7.6023876354,
                    51.9755779545,
                    0
                  ],
                  [
                    7.6023840855,
                    51.9755761099,
                    0
                  ],
                  [
                    7.6023740497,
                    51.975569543,
                    0
                  ],
                  [
                    7.6023732746,
                    51.9755691325,
                    0
                  ],
                  [
                    7.6023724794,
                    51.9755695024,
                    0
                  ],
                  [
                    7.6023573836,
                    51.9755783006,
                    0
                  ],
                  [
                    7.6023540422,
                    51.975579855,
                    0
                  ],
                  [
                    7.6023504724,
                    51.97558077,
                    0
                  ],
                  [
                    7.6023467952,
                    51.9755810144,
                    0
                  ],
                  [
                    7.6023431357,
                    51.9755805801,
                    0
                  ],
                  [
                    7.6023396179,
                    51.9755794816,
                    0
                  ],
                  [
                    7.6023363615,
                    51.9755777564,
                    0
                  ],
                  [
                    7.6023288942,
                    51.9755728699,
                    0
                  ],
                  [
                    7.6023275971,
                    51.9755721827,
                    0
                  ],
                  [
                    7.6023243663,
                    51.9755700466,
                    0
                  ],
                  [
                    7.6023234727,
                    51.9755693223,
                    0
                  ],
                  [
                    7.602285131,
                    51.9755442335,
                    0
                  ],
                  [
                    7.6022843559,
                    51.975543823,
                    0
                  ],
                  [
                    7.6022835607,
                    51.9755441929,
                    0
                  ],
                  [
                    7.6022042514,
                    51.9755904195,
                    0
                  ],
                  [
                    7.6022035414,
                    51.9755909366,
                    0
                  ]
                ]
              ],
              "bbox": null,
              "type": "Polygon"
            }        
    """.trimIndent()

