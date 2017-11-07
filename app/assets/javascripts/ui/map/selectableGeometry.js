define([], function() {

  var STYLE = {
        color       : '#a64a40',
        opacity     : 1,
        fillColor   : '#ff0000',
        fillOpacity : 0.08,
        weight      : 0.7
      };

  var SelectableGeometry = function(geojson, map) {

    var self = this,

        selected = false,

        stashedMapBounds = false,

        select = function() {
          selected = true;
          stashedMapBounds = map.getBounds();
          map.fitBounds(self.getBounds(), {
            paddingTopLeft: [440, 20],
            paddingBottomRight: [20, 20],
            animate: true
          });
        },

        deselect = function() {
          if (stashedMapBounds)
            map.fitBounds(stashedMapBounds);

          selected = false;
          stashedMapBounds = false;
        },

        isSelected = function() {
          return selected;
        };

    this.select = select;
    this.deselect = deselect;
    this.isSelected = isSelected;

    // Inherits from Leaflet's default GeoJSON object
    L.GeoJSON.apply(this, [ geojson, STYLE ]);
  };
  SelectableGeometry.prototype = Object.create(L.GeoJSON.prototype);

  return SelectableGeometry;

});
