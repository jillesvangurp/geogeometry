package com.jillesvangurp.geojson

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JsonHelpersTest {

    @Test
    fun shouldCorrectlyUrlEncode() {
        val cases = mapOf(
            // unreserved chars must stay as-is
            "AZaz09-._~" to "AZaz09-._~",

            // whitespace
            "Hello World" to "Hello%20World",

            // German sharp-s (ß) – 2-byte UTF-8
            "Wattstraße"  to "Wattstra%C3%9Fe",

            // umlaut
            "München"     to "M%C3%BCnchen",

            // euro sign, ampersand, percent
            "€ & %"       to "%E2%82%AC%20%26%20%25",

            // plus and equals
            "a+b=c"       to "a%2Bb%3Dc",

            // slash must be escaped
            "path/part"   to "path%2Fpart",

            // tilde is unreserved
            "~tilde"      to "~tilde",
            // colons and poop emoji
            "::\uD83D\uDCA9" to "%3A%3A%F0%9F%92%A9",
        )

        cases.forEach { (raw, expected) ->
            raw.urlEncode() shouldBe expected
        }
    }
}