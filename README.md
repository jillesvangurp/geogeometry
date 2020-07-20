![Java CI with Gradle](https://github.com/jillesvangurp/geogeometry/workflows/Java%20CI%20with%20Gradle/badge.svg)
[![](https://jitpack.io/v/jillesvangurp/geogeometry.svg)](https://jitpack.io/#jillesvangurp/geogeometry)

# Introduction

GeoGeometry started out as a simple side project to help me come up with a list of geo hashes that cover a particular geo shape. This is a nice thing to be able to do if you want to build search engine functionality. In addition, I added algorithms to solve various geometric problems. Mostly these are well known & documented algorithms of course. But it is nice to have some simple library of implementations for these things.

# History of this project
I initially used Java for this and over time added several implementations of common geometry algorithms. In 2019, after not touching this project for years, I ported the entire code base to **Kotlin**. Minor API changes aside, this should be backwards compatible for most Java users. At this point, the library is a multiplatform Kotlin project. This means it can be compiled to Jvm, Javascript, and native (not currently tested).

## Geohashes

A geo hash is a representation of a coordinate that interleaves the bit representations of the latitude and longitude and base32 encodes the result. This string representation has a very useful property: geo hashes of nearby coordinates will have the same prefix. As is observed in this blog post: http://blog.notdot.net/2009/11/Damn-Cool-Algorithms-Spatial-indexing-with-Quadtrees-and-Hilbert-Curves, geo hashes effectively encode the path to a leaf in a quad tree.

Geohashes are super useful if you are building e.g. search engines. Though if you are, there are better data structures such as Quad Trees or BKD Trees that Elasticsearch uses in more recent versions. 

Here's a [simple example of the hashes for a concave polygon of Berlin](http://geojson.io/#id=gist:jillesvangurp/0e4e13a3c9f118af9c7adecafcd2866f) rendered on a map (courtesy of geojson.io).

# Features

- GeoGeometry class with methods that allow you to:
    - Calculate distance between two coordinates using the *Haversine- algorithm.
    - Calculate distance of a point to a line. lineString, and polygon
    - Calculate the area of a polygon
    - Simplify polygons using the *Douglas Peucker- algorithm
    - check bounding box containment for a point
    - check *polygon containment- for a point
    - get the center for a polygon
    - get bounding box for a polygon
    - convert *circle to a polygon*
    - create a *polygon from a point cloud*
    - *translate a wgs84 coordinate- by x & y meters along the latitude and longitude

- GeoHashUtils class with methods that allow you to: 
    - encode and decode geo hashes; this functionality has been adapted from the original Apache Lucene implementation of this class.
    - check containment of a point in a geohash
    - find out the boundingbox of a geohash
    - find out neighboring geohashes east, west, south, or north of a geohash
    - get the 32 sub geo hashes for a geohash, or the north/south halves, or the NE, NW, SE, SW quarters.
    - cover lines, paths, polygons, or circles with geo hashes

- geojson.kt file with type aliases and model classes that allow you to work with geojson content.

- Kotlin Multiplatform
    - Currently there are two build targets for js and jvm
    - No runtime dependencies other than the kotlin stdlib
    - Native should 'just work' but has not been tested.

# Limitations

- The geohash covering algorithm fills polygons from the inside. Currently, the hash bbox has to be fully contained to be included. One obvious limitation
with this approach is that details that are too small, are not covered by hashes. The solution to that would be either using smaller (i.e. more) hashes or changing the algorithm to also allow partially containment. 
- Some of the algorithms used have limitations with respect to where you can use them. Generally things should be fine around the date line (if not report bugs). However, the poles are tricky and some of the algorithms get inaccurate or simply fail to exit. Because most data sets lack data for the poles, this should not be a big issue for most. Some of the algorithms now throw an exception if you try this. I'm not currently planning a fix but would appreciate pull requests for this.
- Some of the algorithms have quadratic or exponential complexity and you can easily trigger situations where execution time is going to be substantial and/or you run out of memory. For example covering a circle with a radius of a few hundred kilometers with geohashes of length 10 is probably not a great idea. If you are unsure, use getSuitableHashLength() and add 1 or 2 to get good enough granularity.
- Beware of the `[longitude,latitude]` order in geojson arrays vs. the latitude, longitude order when not using arrays. This is based on the (unfortunate) geojson convention of specifying longitude before latitude in arrays. When not using arrays, I use latitude followed by longitude, just like everyone else.
- I try to be good about adding tests but test coverage is not perfect and some of the algorithms have 'interesting' edge-cases. Better algorithms may be available. This is merely a best effort from my side and it works well enough for me. I welcome pull requests to improve things

# Get it 

Use the latest jitpack releases:

[![](https://jitpack.io/v/jillesvangurp/geogeometry.svg)](https://jitpack.io/#jillesvangurp/geogeometry)

# Building from source

It's a gradle project. So, checking it out and doing a `gradle build` should do the trick.

Note. this is a kotlin multiplatform build and currently it produces a javascript build as well as a jvm jar.  Adding IOS native and other platforms should be straightforward as well. The project has no run time dependencies beyond the standard kotlin library.

# License

Like all my other projects, this project is licensed under the so-called MIT license. However, `GeoHashUtils` was copied and adapted from Apache Lucene (a long time ago) and I have retained the Apache License for that file, as required in that license. Both licenses are compatible and should also pose no issues when used with software under most other OSS licenses.

For more details see the [LICENSE](LICENSE) file
