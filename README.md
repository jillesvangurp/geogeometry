![Java CI with Gradle](https://github.com/jillesvangurp/geogeometry/workflows/Java%20CI%20with%20Gradle/badge.svg) [![](https://jitpack.io/v/jillesvangurp/geogeometry.svg)](https://jitpack.io/#jillesvangurp/geogeometry)

# Introduction

GeoGeometry started out as a simple side project while I was building a startup in 2012. The key feature I needed at time was a list of geo hashes that cover a particular geo shape. This is a nice thing to be able to do if you want to build search engine functionality and want to implement geospatial search. Over time, I added algorithms to solve various geometric problems. Mostly these are well known & documented algorithms of course. But it is nice to have some simple library of implementations for these things. I still maintain this library; especially since I became CTO of [FORMATION](https://tryformation.com) where we use it with our indoor maps for things like geofences, georeferencing coordinates from various location providers, etc.

I initially used Java for this and over time added several implementations of common geometry algorithms. In 2019, after not touching this project for years, I ported the entire code base to **Kotlin**. Minor API changes aside, this should be backwards compatible for most Java users. At this point, the library is a multiplatform Kotlin project. 

# Get it

[![](https://jitpack.io/v/jillesvangurp/geogeometry.svg)](https://jitpack.io/#jillesvangurp/geogeometry)

The jar files are hosted on jitpack. Check their instructions for adding this to your maven/gradle build.

## About Geohashes

A geo hash is a representation of a coordinate that interleaves the bit representations of the latitude and longitude and base32 encodes the result. This string representation has a very useful property: geo hashes of nearby coordinates will have the same prefix. As is observed in this blog post: http://blog.notdot.net/2009/11/Damn-Cool-Algorithms-Spatial-indexing-with-Quadtrees-and-Hilbert-Curves, geo hashes effectively encode the path to a leaf in a quad tree.

Geohashes are super useful if you are building e.g. search engines. Though if you are, there are better data structures such as Quad Trees or BKD Trees that Elasticsearch uses in more recent versions. 

Here's a [simple example of the hashes for a concave polygon of Berlin](http://geojson.io/#id=gist:jillesvangurp/0e4e13a3c9f118af9c7adecafcd2866f) rendered on a map (courtesy of geojson.io). These hashes are calculated with the algorithm in `GeoHashUtils`. You could put these hashes into a database or search engine to implement polygon search. 

# Features

- GeoGeometry class with methods that allow you to:
  - **Calculate distance** between two coordinates using the **Haversine** or **Vicenty** algorithm
  - Calculate perpendicular distance of a point to a line. lineString, and polygon
  - Calculate the **area** of a polygon
  - **Simplify polygons** using the **Douglas Peucker** - algorithm. Sometimes polygons can be very detailed, which makes handling them very CPU/memory intensive. E.g. some polygons for New Zealand are more than 200MB.
  - check bounding box containment for a point
  - check **polygon containment** for a point
  - get the center for a polygon
  - get bounding box for a polygon
  - convert a **circle to a polygon**
  - create a **polygon from a point cloud**
  - **translate a wgs84 coordinate** by x & y meters along the latitude and longitude

- GeoHashUtils class with methods that allow you to: 
  - **encode and decode** geo hashes; this functionality has been adapted from the original Apache Lucene implementation of this class.
  - find out the **boundingbox** of a geohash   
  - check **containment** of a point in a geohash (must have the hash as a prefix of its own hash).
  - find out **neighboring geohashes** east, west, south, or north of a geohash.
  - get the **32 sub geo hashes** for a geohash, or the north/south halves, or the NE, NW, SE, SW quarters.
  - **cover shapes** like lines, paths, polygons, or circles with geo hashes for indexing purposes.

- geojson classes to easily work with GeoJson, which just like this library uses arrays of doubles as the core primitive. We use kotlinx.serialization for parsing and serializing this so this works across platforms as well!

- Kotlin Multiplatform
  - Currently there are two build targets for js and jvm. I may add native targets later; this should just work but I have no easy way to test this.
  - No runtime dependencies other than the kotlin stdlib
  - Adding native targets should 'just work' but has not been tested.

# Limitations

- Some of the algorithms used have limitations with respect to where you can use them. Generally things should be fine around the date line (if not report bugs). However, the poles are tricky and some of the algorithms get inaccurate or simply fail to exit. Because most data sets lack data for the poles, this should not be a big issue for most. Some of the algorithms now throw an exception if you try this. I'm not currently planning a fix but would appreciate pull requests for this.
- Some of the algorithms have quadratic or exponential complexity and you can easily trigger situations where execution time is going to be substantial and/or you run out of memory. For example covering a circle with a radius of a few hundred kilometers with geohashes of length 10 is probably not a great idea. If you are unsure, use getSuitableHashLength() and add 1 or 2 to get good enough granularity.
- Beware of the `[longitude,latitude]` order in geojson arrays vs. the latitude, longitude order when not using arrays. This is based on the (unfortunate) geojson convention of specifying longitude before latitude in arrays. When not using arrays, I use latitude followed by longitude, just like everyone else.
- I try to be good about adding tests but test coverage is not perfect and some of the algorithms have 'interesting' edge-cases. Better algorithms may be available. This is merely a best effort from my side and it works well enough for me. I welcome pull requests to improve things

# Building from source

It's a gradle project. So, checking it out and doing a `gradle build` should do the trick.

Note. this is a kotlin multiplatform build and currently it produces a javascript build as well as a jvm jar.  Adding IOS native and other platforms should be straightforward as well. The project has no run time dependencies beyond the standard kotlin library.

# License

Like all my other projects, this project is licensed under the so-called MIT license. However, `GeoHashUtils` was copied and adapted from Apache Lucene (a long time ago) and I have retained the Apache License for that file, as required in that license. Both licenses are compatible and should also pose no issues when used with software under most other OSS licenses.

For more details see the [LICENSE](LICENSE) file
