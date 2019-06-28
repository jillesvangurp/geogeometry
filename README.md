# Introduction

GeoGeometry started out as a simple side project to help me come up with a list of geo hashes that cover a particular geo shape. However, most of the recent work has actually gone into adding several implementations of common geometry algorithms.

GeoGeometry is of course not the only implementation of these algorithms. However, it is unique in the way it has been designed and the simplicity of using it.

There are two driving design principles:

- It assumes you are using something like geojson, which is a convention for representing geometric shapes on the web. One key feature of *geojson* is that all shapes are represented as multi dimensional arrays of doubles. This library does the same. 
- It avoids the trap of object orientation. Object orientation and geometry go way back. The first object oriented systems were all  about cute little Point and Line classes. As a consequence, world + dog now feels compelled to come up with their own Point, Line, Polygon, etc. classes. So, this library has *no classes that you can instantiate and only provides static methods*. This makes it easy to integrate whatever framework you have for representing shapes with geogeometry. Also, it makes it trivial to port the code to different languages. For example, checkout my partial port for [javascript](https://github.com/jillesvangurp/geotools-js) and [php"](https://github.com/jillesvangurp/geotools-php). These implementations are a little behind the java implementation because I don't actively use them currently. Not creating insane amounts of point objects helps keep things fast as well and can save a ton of memory.

## Geohashes

A geo hash is a representation of a coordinate that interleaves the bit representations of the latitude and longitude and base32 encodes the result. This string representation has a very useful property: geo hashes of nearby coordinates will have the same prefix. As is observed in this blog post: http://blog.notdot.net/2009/11/Damn-Cool-Algorithms-Spatial-indexing-with-Quadtrees-and-Hilbert-Curves, geo hashes effectively encode the path to a leaf in a quad tree.

Geohashes are super useful if you are building e.g. search engines. Though if you are, there are better data structures such as Quad Trees or BKD Trees that Elasticsearch uses in more recent versions.

## Name

Note. Geogeometry was formerly known as geotools. I renamed the project because the name was already in use by geotools.org, which predates this project and on top of that delivers very similar functionality for Java. 

# Features

* GeoGeometry class with methods that allow you to:
    * Calculate distance between two coordinates using the *Haversine* algorithm.
    * Calculate distance of a point to a line. lineString, and polygon
    * Calculate the area of a polygon
    * Simplify polygons using the *Douglas Peucker* algorithm
    * check bounding box containment for a point
    * check *polygon containment* for a point
    * get the center for a polygon
    * get bounding box for a polygon
    * convert *circle to a polygon*
    * create a *polygon from a point cloud*
    * *translate a wgs84 coordinate* by x & y meters along the latitude and longitude

* GeoHashUtils class with methods that allow you to: 
    * encode and decode geo hashes; this functionality has been adapted from the original Apache Lucene implementation of this class.
    * check containment of a point in a geohash
    * find out the boundingbox of a geohash
    * find out neighboring geohashes east, west, south, or north of a geohash
    * get the 32 sub geo hashes for a geohash, or the north/south halves, or the NE, NW, SE, SW quarters.
    * cover lines, paths, polygons, or circles with geo hashes

# Limitations

* Some of the algorithms used have limitations with respect to where you can use them. Generally things should be fine around the date line (if not report bugs). However, the poles are tricky and some of the algorithms get inaccurate or simply fail to exit. Because most data sets lack data for the poles, this should not be a big issue for most. Some of the algorithms now throw an exception if you try this. I'm not currently planning a fix but would appreciate pull requests for this.
* There is currently not a whole lot of input validation in this library. That means some of the algorithms might do unexpected things if you call them in the wrong way. Input validation would make using this library more expensive and that is something I'm trying to avoid.
* Some of the algorithms have quadratic or exponential complexity and you can easily trigger situations where execution time is going to be substantial and/or you run out of memory. For example covering a circle with a radius of a few hundred kilometers with geohashes of length 10 is probably not a great idea. If you are unsure, use getSuitableHashLength() and add 1 or 2 to get good enough granularity.
* Beware of the [longitude,latitude] order in arrays vs. the latitude, longitude order when not using arrays. This is based on the (unfortunate) geojson convention of specifying longitude before latitude in arrays. When not using arrays, I use latitude followed by longitude, just like everyone else.

# Get it 

Use the latest jitpack releases:

[jitpack releases](https://jitpack.io/#jillesvangurp/geogeometry)

# Building from source

It's a maven project. So, checking it out and doing a mvn clean install should do the trick.

Alternatively, you can exercise your rights under the license and simply copy and adapt as needed. The [license](https://github.com/jillesvangurp/geogeometry/blob/master/LICENSE) allows you to do this and I have no problems with this although I do appreciate attribution.

Should anyone like this licensed differently, please contact me.

If anyone wants to fix stuff just send me a pull request.

# License

Like all my other projects, this project is licensed under the so-called MIT license. However, GeoHashUtils.java was copied and adapted from Apache Lucene and I have retained the Apache License for that file, as required in that license. Both licenses are compatible and should also pose no issues when used with software under most other OSS licenses.

For more details see the LICENSE file

# Changelog

**Check the Github Releases** tab for newer releases. Below is for older releases.


* 2.11
    * Release to maven central and change groupid
* 2.10
    * relax coordinate validation to not fail on very small rounding errors
* 2.7-2.9
    * fix subtle bug with perpendicular distance to line for horizontal lines and polygons
    * add implementation of Douglas Peucker to simplify polygons with large amounts of segments
    * add methods to calculate bounding boxes for all shape types, add method to calculate area for bounding box
    * close polygon with the starting point when calculating a polygon for a point cloud
* 2.6
    * add function to calculate polygon area
    * fix distance calculation bug with line segments (was incorrectly using intersection outside segment)
    * fix east/west bug on 180 longitude since encode can no longer work with invalid longitudes
* 2.1
    * Add distance functions for lines, lineStrings, and polygons.
* 2.0 - Geojson & project rename: geotools becomes geogeometry
    * There was an existing, high profile project out there called geotools. So I renamed my project to geogeometry.
    * Adapt the API to comply with geojson wherever possible. This means polygons are 3d arrays and points are [longitude,latitude] instead of the other way around. This is a major, API breaking change that mostly affects GeoGeometry.
    * Clean up API
* 1.5
    * misc bug fixes
    * close circle polygon with start coordinate
    * expandPolygon, contains, and overlaps methods added
    * add some ifs to prevent infinite loops near the poles
    * method to filter point clouds by dropping the x% furthest points

* 1.4
    * many bug fixes & misc refactoring
    * improve covering polygon with geohashes
    * improve getSuitableHashLength
    * add method to convert degrees with minutes and seconds to their decimal equivalent
    * replace the algorithm for calculating a polygon from points with a better one
    * simple method for calculating centroid of a polygon
* 1.3
    * Fix major bug that happens nea longitude 180, several methods were affected by this one.
    * Remove dependency on java 1.7; should now work fine with 1.6 as well. I may revert this at some point.
    * isEast,isWest,isNorth,isSouth methods added
* 1.2
    * Vastly expanded functionality in both classes to support basic geometric shapes and covering those with geo hashes.
    * Major refactoring and code and API cleanup
* 1.1
    * Added GeoGeometry class for manipulating bounding boxes and polygons of wgs 84 coordinates
    * Improved GeoHashUtils with several new methods
    * Merged GeoDistance into GeoGeometry
* 1.0
    * first release of GeoTools



[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/jillesvangurp/geogeometry/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

