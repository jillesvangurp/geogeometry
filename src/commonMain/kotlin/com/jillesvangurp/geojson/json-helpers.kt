package com.jillesvangurp.geojson

import com.jillesvangurp.serializationext.DEFAULT_JSON
import kotlinx.serialization.encodeToString


val FeatureCollection.geoJsonIOUrl: Any
    get() {
        return DEFAULT_JSON.encodeToString(this).let { json ->
            "https://geojson.io/#data=${"data:application/json,$json".urlEncode()}"
        }
    }

val Geometry.geoJsonIOUrl get() = this.asFeatureCollection.geoJsonIOUrl

fun String.urlEncode(): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~')
    return buildString {
        this@urlEncode.forEach { char ->
            if (char in allowedChars) {
                append(char)
            } else {
                append(char.code.toByte().toInt().let {
                    "%${it.shr(4).and(0xF).toString(16)}${it.and(0xF).toString(16)}"
                }.uppercase())
            }
        }
    }
}