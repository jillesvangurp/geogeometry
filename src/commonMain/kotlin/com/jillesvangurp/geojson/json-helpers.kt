package com.jillesvangurp.geojson

import com.jillesvangurp.serializationext.DEFAULT_JSON


val FeatureCollection.geoJsonIOUrl: Any
    get() {
        return DEFAULT_JSON.encodeToString(this).let { json ->
            "https://geojson.io/#data=${"data:application/json,$json".urlEncode()}"
        }
    }

val Geometry.geoJsonIOUrl get() = this.asFeatureCollection.geoJsonIOUrl

private const val HEX = "0123456789ABCDEF"

/** Percent-encode according to RFC 3986 (UTF-8) */
fun String.urlEncode(): String = buildString {
    // including this to avoid adding more library dependencies; use something proper if this causes you grief.
    for (byte in encodeToByteArray()) {          // UTF-8 bytes
        val b = byte.toInt() and 0xFF            // 0-255, no sign
        val c = b.toChar()
        if (c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz0123456789-._~"
        ) {                   // keep unreserved
            append(c)
        } else {                                 // escape everything else
            append('%')
            append(HEX[b ushr 4])
            append(HEX[b and 0x0F])
        }
    }
}