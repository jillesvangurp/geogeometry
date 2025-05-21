[![](https://jitpack.io/v/jillesvangurp/geogeometry.svg)](https://jitpack.io/#jillesvangurp/geogeometry)![Java CI with Gradle](https://github.com/jillesvangurp/geogeometry/workflows/Java%20CI%20with%20Gradle/badge.svg)

# Introduction

Geogeometry is a collection of geospatial geometry algorithms and utilities in Kotlin. This library is what we use at FORMATION, which is a company that I'm the CTO of that deals with a lot of geospatial data. Mostly, whenever I do something that looks like it could be open sourced and is vaguely geo related, I put it in this library. A lot of this stuff is available in third party libraries for (mainly) Java. However, those libraries have two limitations that this library addresses: 

- They are Java centric and generally have a terrible developer experience for Kotlin developers (or anyone really). This library is very Kotlin centric and uses things like extension functions, type aliases, etc. to make life easy for Kotlin developers. I'm not bragging when I say that this is orders of magnitude nicer to use than most of what is out there. And if you find something nicer, please file an issue and I might fix it.
- They only work on the JVM. This library a kotlin multi-platform library and works on all Kotlin platforms. We use it heavily with kotlin-js and the jvm. It should work fine on native IOS, Linux, Windows, etc. and WASM. I.e. basically everywhere. This is probably the **#1 multiplatform library for geospatial** stuff on Kotlin currently.

# History

GeoGeometry started out as a simple side project while I was building my first startup in 2012. The key feature I needed at time was a list of geohashes that cover a particular geo shape. This is a nice thing to be able to do if you want to build search engine functionality and want to implement geospatial search. Over time, I added algorithms to solve various geometric problems. Mostly these are well known & documented algorithms of course. But it is nice to have some simple library of implementations for these things. I still maintain this library; especially since I became CTO of [FORMATION](https://tryformation.com) where we use it with our indoor maps for things like geofences, geo-referencing coordinates from various location providers, etc.

I initially used Java for this and over time added several implementations of common geometry algorithms. In 2019, after not touching this project for some years, I ported the entire code base to **Kotlin**. And soon after I discovered Kotlin multiplatform and got rid of all Java dependencies.

# Get It

[![](https://jitpack.io/v/jillesvangurp/geogeometry.svg)](https://jitpack.io/#jillesvangurp/geogeometry)

This is a Kotlin multi-platform distribution supporting all major compilation targets for Kotlin, including jvm, js, wasm, ios, linux, mac, windows, etc.

We use our own maven repo for distributing this: Add the repository:

```kotlin
repositories {
  mavenCentral()
  maven { url = uri("https://maven.tryformation.com/releases") }
}
```

and then add the dependency:

```kotlin
implementation("com.github.jillesvangurp:geogeometry:<VERSION>")
```

You can find the latest version in the [releases section](https://github.com/jillesvangurp/geogeometry/releases).

# Features

- Simple API, most functions work standalone and use simple input types
- Kotlin Multiplatform
  - Currently there are several build targets. More may be added later. I could use some help with mobile targets.
  - No runtime dependencies other than the kotlin stdlib
  - Adding more targets should 'just work' but has not been tested.
- Extensive support for Geojson style data. Comes with it a kotlinx.serialization ready sealed class for handling geojson. And lots of extension functions for doing geometric things with your geojson geometries. 

For a few of the algorithms in this code base, I've adapted / been inspired by code from others. This work would not be possible without that and I try to credit any such sources in the code base. And of course with all the AI craze, more than a few algorithms and tests have been enhanced with the sometimes helpful suggestions of various LLMs.

## GeoGeometry class

GeoGeometry class with lots of functions that allow you to:
- **Calculate distance** between two coordinates using the **Haversine** or **Vicenty** algorithm
- Calculate perpendicular distance of a point to a line. lineString, and polygon
- Calculate the **area** of a polygon
- **Simplify polygons** using the **Douglas Peucker** - algorithm. Sometimes polygons can be very detailed, which makes handling them very CPU/memory intensive. E.g. some polygons for New Zealand are more than 200MB.
- check bounding box containment for a point
- check **polygon containment** for a point
- get the center for a polygon
- get bounding box for a polygon
- convert a **circle to a polygon**
- create a **polygon from a point cloud**. An algorithm for a convex hull is included and another experimental one for a concave hull.
- **translate a wgs84 coordinate** by x & y meters along the latitude and longitude
- **rotate** a point around another point

## GeoHashUtils

GeoHashUtils class with methods that allow you to:

- **encode and decode** geo hashes; this functionality has been adapted from the original Apache Lucene implementation of this class.
- find out the **boundingbox** of a geohash   
- check **containment** of a point in a geohash (must have the hash as a prefix of its own hash).
- find out **neighboring geohashes** east, west, south, or north of a geohash.
- get the **32 sub geo hashes** for a geohash, or the north/south halves, or the NE, NW, SE, SW quarters.
- **cover shapes** like lines, paths, polygons, or circles with geo hashes for indexing purposes.

## GeoJson support

Geojson classes are provided that allow you to easily work with GeoJson, which just like this library uses arrays of doubles as the core primitive. We use **kotlinx.serialization** for parsing and serializing this so this works across all Kotlin platforms.
  
- sealed class hierarchy to represent the various geometries
- uses type aliases to distinguish the different coordinate arrays
- translate, scaleX, scaleY, and rotate to transform any Geojson Geometry
- contains / intersects / distance algorithms
- calculate a centroid for a shape
- lots of extension functions to make working with geometries easy
- convenient `geojsonIoUrl` extension function that allows you to check a geometry or feature collection on Mapboxes' https://geojson.io tool. 

## Coordinate conversions

Convert between WGS 84 coordinates and UTM, UPS, MGRS, and USNG style coordinates.

I put quite a bit of effort in doing code archeology to combine bits and pieces of various old Java implementations for this. Lots of edge cases and "you just have to know why" bits of code. As a consequence, I have some nice robust tests around this and all seems to work. 

I also used this [Coordinates converter](https://coordinates-converter.com) to verify my converters produce the same results. Also, I use lots of randomized points to ensure round trip conversions end up being close to where they should be and squashed lots of edge cases in the process. Feedback and pull requests to improve this further are welcome

  - extension functions to convert to and from [UTM coordinates](https://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system)  and [UPS coordinates](https://en.wikipedia.org/wiki/Universal_polar_stereographic_coordinate_system).
  - Conversion to and from [MGRS](https://en.wikipedia.org/wiki/Military_Grid_Reference_System) / [USNG](https://en.wikipedia.org/wiki/United_States_National_Grid) format

## Map Tile support

Doing things with map tiles is useful if you work with a lot of map content and useful if you are looking to use e.g. Elasticsearch with its support for `geotile_grid` aggregations or filter content by tile path.

- Tile class that represents tiles used in e.g. Openstreetmap and Google Maps. Useful for converting between coordinates, zoom levels and tile coordinates at different zoom levels.
- Lots of extension functions to calculate parent tiles, quad keys in string or long format, etc.

## About Geo Hashes

A geo hash is a representation of a coordinate that interleaves the bit representations of the latitude and longitude and base32 encodes the result. This string representation has a very useful property: geo hashes of nearby coordinates will have the same prefix. As is observed in this blog post: http://blog.notdot.net/2009/11/Damn-Cool-Algorithms-Spatial-indexing-with-Quadtrees-and-Hilbert-Curves, geo hashes effectively encode the path to a leaf in a quad tree.

Geohashes are super useful if you are building e.g. search engines. Though if you are, there are better data structures such as Quad Trees or BKD Trees that Elasticsearch uses in more recent versions.

Here's a [simple example of the hashes for a concave polygon of Berlin](http://geojson.io/#id=gist:jillesvangurp/0e4e13a3c9f118af9c7adecafcd2866f) rendered on a map (courtesy of geojson.io). These hashes are calculated with the algorithm in `GeoHashUtils`. You could put these hashes into a database or search engine to implement polygon search.

# Limitations

I've been maintaining and using this library for many years now. So that means I'm both mildy confident in what it does and humbled by the experience of finding and fixing lots of bugs. So, I'm not going to claim that it is all perfect. I regularly find and fix more bugs.

When in doubt, add tests. That's what I do. With geospatial geometry algorithms there are lots of edge cases. Math around the poles gets funny. There's the antimeridian. Etc. Mostly I try to cover the antimeridian with tests but some of the algorithms are known to not work near the poles.

Bearing that in mind:

- I have validation for things like coordinates but it's not used everywhere. Validation has a price and it's not always worth it. Things work better if you don't send latitudes and longitudes that are clearly out of range.
- Some algorithms are not quadratic or exponential in complexity and won't terminate if you call them with the wrong level of ambition. Don't do that. 
- The order of latitude and longitude (alphabetical) or longitude and latitude is a problem. The library uses a lot of Geojson inspired data structures where the order is definitely `[longitude, latitude]`. In functions where these are separated out, I use `latitude, longitude`. Beware of this when mixing and use named arguments and the friendly `.latitude` and `.longitude` extension properties I have for `Point` which is a typealias for `doubleArray`.

# Building from source

It's a gradle project. So, checking it out and doing a `gradle build` should do the trick.

Currently most kotlin platforms are supported. Not all of them are tested properly because of test tooling issues. And because I hate waiting for all the tests to run. It's all simple math that hopefully works the same on each platform.

# License

Like all my other projects, this project is licensed under the so-called MIT license. 

However, `GeoHashUtils` was copied and adapted from Apache Lucene (a long time ago) and I have retained the Apache License for that file, as required in that license. The rest of this repository is MIT licensed. Both licenses are compatible and should also pose no issues when used with software under most other OSS licenses.

For more details see the [LICENSE](LICENSE) file

