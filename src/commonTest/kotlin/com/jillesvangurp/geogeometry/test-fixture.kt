package com.jillesvangurp.geogeometry

val bergstr16Berlin = doubleArrayOf(13.3941763, 52.5298311)
val brandenBurgerGate = doubleArrayOf(13.377157, 52.516279)
val potsDammerPlatz = doubleArrayOf(13.376599, 52.509515)
val moritzPlatz = doubleArrayOf(13.410717, 52.503663)
val senefelderPlatz = doubleArrayOf(13.412949, 52.532755)
val naturkundeMuseum = doubleArrayOf(13.381921, 52.531188)
val rosenthalerPlatz = doubleArrayOf(13.401361, 52.529948)
val oranienburgerTor = doubleArrayOf(13.38707, 52.525339)

val bigRing = arrayOf(potsDammerPlatz,brandenBurgerGate,naturkundeMuseum,senefelderPlatz,moritzPlatz,potsDammerPlatz)
val smallRing = arrayOf(rosenthalerPlatz,oranienburgerTor,bergstr16Berlin,rosenthalerPlatz)

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