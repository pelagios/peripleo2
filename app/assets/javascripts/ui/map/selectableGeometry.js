define([], function() {

  var STYLE = {
        color       : '#a64a40',
        opacity     : 1,
        fillColor   : '#ff0000',
        fillOpacity : 0.08,
        weight      : 0.7
      };

  var SelectableGeometry = function(geojson) {

    var select = function() {

        },

        deselect = function() {

        },

        isSelected = function() {

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
