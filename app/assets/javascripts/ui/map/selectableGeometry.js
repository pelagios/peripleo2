define([], function() {

  var STYLE = {
        color       : '#a64a40',
        opacity     : 1,
        fillColor   : '#ff0000',
        fillOpacity : 0.08,
        weight      : 0.7
      };

  var SelectableGeometry = function(geojson, map) {

    var selected = false,

        select = function() {
          selected = true;
        },

        deselect = function() {
          selected = false;
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
