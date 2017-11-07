define([], function() {

  return {

    /** Assuming polygon shapes, this method returns those that intersect the current viewport **/
    getVisibleShapes : function(map, featureCollection) {
      var visible = [],
          bounds = map.getBounds();

      featureCollection.eachLayer(function(l) {
        if (bounds.overlaps(l.getBounds()))
          visible.push(l);
      });

      return visible;
    },

    /** Computes the intersection between two overlapping bounds **/
    intersectBounds : function(a, b) {
      var neA = a.getNorthEast(),
          swA = a.getSouthWest(),

          topA = neA.lat, rightA = neA.lng, bottomA = swA.lat, leftA = swA.lng,

          neB = b.getNorthEast(),
          swB = b.getSouthWest(),

          topB = neB.lat, rightB = neB.lng, bottomB = swB.lat, leftB = swB.lng,

          top    = (topB < topA) ? topB : topA,
          right  = (rightB < rightA) ? rightB : rightA,
          bottom = (bottomB > bottomA) ? bottomB : bottomA,
          left   = (leftB > leftA) ? leftB : leftA;

      return L.latLngBounds([ bottom, left ], [ top, right ]);
    },

    getSize : function(bounds) {
      var ne = bounds.getNorthEast(),
          sw = bounds.getSouthWest(),
          width = ne.lng - sw.lng,
          height = ne.lat - sw.lat;

      return width * height;
    }

  };

});
