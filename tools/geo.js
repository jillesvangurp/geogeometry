var DEFAULT_PRECISION = 12;
var BITS = [16, 8, 4, 2, 1];
var BASE32_CHARS = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'];
var BASE32_DECODE_MAP = {};
for ( i = 0; i < BASE32_CHARS.length; i++) {
    BASE32_DECODE_MAP[BASE32_CHARS[i]] = i;
};


var decode_bbox = function(geohash) {
    var lat_interval = [-90.0, 90.0];
    var lon_interval = [-180.0, 180.0];

    var is_even = true;

    for (var i = 0; i < geohash.length; i++) {
        var currentChar = BASE32_DECODE_MAP[geohash[i]];
        for (var z = 0; z < BITS.length; z++) {
            var mask = BITS[z];
            if (is_even) {
                if ((currentChar & mask) != 0) {
                    lon_interval[0] = (lon_interval[0] + lon_interval[1]) / 2;
                } else {
                    lon_interval[1] = (lon_interval[0] + lon_interval[1]) / 2;
                }

            } else {
                if ((currentChar & mask) != 0) {
                    lat_interval[0] = (lat_interval[0] + lat_interval[1]) / 2;
                } else {
                    lat_interval[1] = (lat_interval[0] + lat_interval[1]) / 2;
                }
            }
            is_even = is_even ? false : true;
        }
    };
    var latitude = (lat_interval[0] + lat_interval[1]) / 2;
    var longitude = (lon_interval[0] + lon_interval[1]) / 2;

    return [lat_interval[0], lat_interval[1], lon_interval[0], lon_interval[1]];
};

var decode = function(geohash) {
    var lat_interval = [-90.0, 90.0];
    var lon_interval = [-180.0, 180.0];

    var is_even = true;

    for (var i = 0; i < geohash.length; i++) {
        var currentChar = BASE32_DECODE_MAP[geohash[i]];
        for (var z = 0; z < BITS.length; z++) {
            var mask = BITS[z];
            if (is_even) {
                if ((currentChar & mask) != 0) {
                    lon_interval[0] = (lon_interval[0] + lon_interval[1]) / 2;
                } else {
                    lon_interval[1] = (lon_interval[0] + lon_interval[1]) / 2;
                }

            } else {
                if ((currentChar & mask) != 0) {
                    lat_interval[0] = (lat_interval[0] + lat_interval[1]) / 2;
                } else {
                    lat_interval[1] = (lat_interval[0] + lat_interval[1]) / 2;
                }
            }
            is_even = is_even ? false : true;
        }
    };
    var latitude = (lat_interval[0] + lat_interval[1]) / 2;
    var longitude = (lon_interval[0] + lon_interval[1]) / 2;

    return [latitude, longitude];
};

// var hashes=["r7hg9z","r7hgcb","r7hgdj","r7hgdn","r7hgdp","r7hgdq","r7hgdr","r7hgf0","r7hgf1","r7hgf2"];
var hashes=["r7hg9ve","r7hg9vf","r7hg9vg","r7hg9vj","r7hg9vk","r7hg9vm","r7hg9vn","r7hg9vp","r7hg9vq","r7hg9vr","r7hg9vs","r7hg9vt","r7hg9vu","r7hg9vv","r7hg9vw","r7hg9vx","r7hg9vy","r7hg9vz","r7hg9wx","r7hg9wz","r7hg9xp","r7hg9xq","r7hg9xr","r7hg9xw","r7hg9xx","r7hg9xy","r7hg9xz","r7hg9y1","r7hg9y2","r7hg9y3","r7hg9y4","r7hg9y5","r7hg9y6","r7hg9y7","r7hg9y8","r7hg9y9","r7hg9yb","r7hg9yc","r7hg9yd","r7hg9ye","r7hg9yf","r7hg9yg","r7hg9yh","r7hg9yj","r7hg9yk","r7hg9ym","r7hg9yn","r7hg9yp","r7hg9yq","r7hg9yr","r7hg9ys","r7hg9yt","r7hg9yu","r7hg9yv","r7hg9yw","r7hg9yx","r7hg9yy","r7hg9yz","r7hg9z","r7hgc8n","r7hgc8p","r7hgc8r","r7hgc8x","r7hgc8z","r7hgcb","r7hgcc0","r7hgcc1","r7hgcc2","r7hgcc3","r7hgcc4","r7hgcc5","r7hgcc6","r7hgcc7","r7hgcc9","r7hgccd","r7hgcce","r7hgccf","r7hgccg","r7hgcch","r7hgccj","r7hgcck","r7hgccm","r7hgccn","r7hgccp","r7hgccq","r7hgccr","r7hgccs","r7hgcct","r7hgccu","r7hgccv","r7hgccw","r7hgccx","r7hgccy","r7hgccz","r7hgcf5","r7hgcfh","r7hgcfj","r7hgcfm","r7hgcfn","r7hgcfp","r7hgcfq","r7hgcfr","r7hgcfx","r7hgdhb","r7hgdhc","r7hgdhf","r7hgdhg","r7hgdhu","r7hgdhv","r7hgdhy","r7hgdhz","r7hgdj","r7hgdkb","r7hgdm0","r7hgdm1","r7hgdm2","r7hgdm3","r7hgdm4","r7hgdm5","r7hgdm6","r7hgdm7","r7hgdm8","r7hgdm9","r7hgdmb","r7hgdmc","r7hgdmd","r7hgdme","r7hgdmf","r7hgdmg","r7hgdmk","r7hgdms","r7hgdmt","r7hgdmu","r7hgdmv","r7hgdmw","r7hgdmy","r7hgdmz","r7hgdn","r7hgdp","r7hgdq","r7hgdr","r7hgdw2","r7hgdw8","r7hgdw9","r7hgdwb","r7hgdwc","r7hgdx0","r7hgdx1","r7hgdx2","r7hgdx3","r7hgdx6","r7hgdx8","r7hgdx9","r7hgdxb","r7hgdxc","r7hgdxd","r7hgdxf","r7hgf0","r7hgf1","r7hgf2","r7hgf30","r7hgf31","r7hgf32","r7hgf33","r7hgf34","r7hgf35","r7hgf36","r7hgf37","r7hgf38","r7hgf39","r7hgf3b","r7hgf3c","r7hgf3d","r7hgf3e","r7hgf3f","r7hgf3g","r7hgf3h","r7hgf3j","r7hgf3k","r7hgf3m","r7hgf3n","r7hgf3p","r7hgf3q","r7hgf3r","r7hgf3s","r7hgf3t","r7hgf3u","r7hgf3v","r7hgf3w","r7hgf3x","r7hgf3y","r7hgf40","r7hgf41","r7hgf42","r7hgf43","r7hgf44","r7hgf45","r7hgf46","r7hgf47","r7hgf48","r7hgf49","r7hgf4d","r7hgf4e","r7hgf4f","r7hgf4g","r7hgf4h","r7hgf4j","r7hgf4k","r7hgf4m","r7hgf4n","r7hgf4p","r7hgf4q","r7hgf4r","r7hgf4s","r7hgf4t","r7hgf4u","r7hgf4v","r7hgf4w","r7hgf4x","r7hgf4y","r7hgf60","r7hgf61","r7hgf62","r7hgf63","r7hgf64","r7hgf65","r7hgf66","r7hgf67","r7hgf68","r7hgf69","r7hgf6h","r7hgf6j","r7hgf80","r7hgf81","r7hgf82","r7hgf83","r7hgf84","r7hgf88","r7hgf89","r7hgf8b","r7hgf8c","r7hgf90","r7hgf92"];

function initialize() {

var medianHash=hashes[Math.floor(hashes.length/2)];
var bboxHash=decode_bbox(medianHash);
var center = new google.maps.LatLng(bboxHash[0], bboxHash[2])

var mapOptions = {
  zoom: 13,
  center: center,
  mapTypeId: google.maps.MapTypeId.ROADMAP
};

var map = new google.maps.Map(document.getElementById('map_canvas'),
    mapOptions);


try {
    for(var i=0;i<hashes.length;i++) {
        var h=hashes[i];
        var bbox=decode_bbox(h);
        var bbox_bounds=new google.maps.LatLngBounds(new google.maps.LatLng(bbox[0], bbox[2]), new google.maps.LatLng(bbox[1], bbox[3]));
        var rectangle = new google.maps.Rectangle();
        rectangle.setOptions({
            strokeColor: '#FF0000',
            strokeOpacity: 0.8,
            strokeWeight: 2,
            fillColor: '#FF0000',
            fillOpacity: 0.35,
            map: map,
            bounds: bbox_bounds
        });
    }
} catch(e) {
    alert(e);
}

}
