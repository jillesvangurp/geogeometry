package com.jillesvangurp.geojson

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val DEFAULT_JSON: Json by lazy {
    Json {
        // don't rely on external systems being written in kotlin or even having a language with default values
        // the default of false is FFing insane and dangerous
        encodeDefaults = true
        // save space
        prettyPrint = false
        // people adding shit to the json is OK, we're forward compatible and will just ignore it
        isLenient = true
        // encoding nulls is meaningless and a waste of space.
        explicitNulls = false
        // adding enum values is OK even if older clients won't understand it
        ignoreUnknownKeys = true
    }
}

val DEFAULT_JSON_PRETTY: Json by lazy {
    Json {
        // don't rely on external systems being written in kotlin or even having a language with default values
        // the default of false is FFing insane and dangerous
        encodeDefaults = true
        // save space
        prettyPrint = false
        // people adding shit to the json is OK, we're forward compatible and will just ignore it
        isLenient = true
        // encoding nulls is meaningless and a waste of space.
        explicitNulls = false
        // adding enum values is OK even if older clients won't understand it
        ignoreUnknownKeys = true
        prettyPrint = true
    }
}


val FeatureCollection.geoJsonIOUrl
    get() = DEFAULT_JSON.encodeToString(this).let { json ->
        "https://geojson.io/#data=${"data:application/json,$json".urlEncode()}"
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