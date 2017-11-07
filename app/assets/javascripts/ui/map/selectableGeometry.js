define([], function() {

  var STYLE = {
        color       : '#a64a40',
        opacity     : 1,
        fillColor   : '#ff0000',
        fillOpacity : 0.08,
        weight      : 0.7
      },

      FIT_OPTIONS = {
        paddingTopLeft: [440, 20],
        paddingBottomRight: [20, 20],
        animate: true
      };

  var SelectableGeometry = function(geojson, featureGroup) {

    var self = this,

        map = featureGroup._map,

        selected = false,

        visible = true,

        select = function() {
          selected = true;
          map.fitBounds(self.getBounds(), FIT_OPTIONS);
        },

        deselect = function() {
          selected = false;
        },

        isSelected = function() {
          return selected;
        },

        show = function() {
          if (!visible) {
            self.addTo(map);
            featureGroup.bringToBack();
          }

          visible = true;
        },

        hide = function() {
          if (visible)
            self.remove();

          visible = false;
        };

    this.select = select;
    this.deselect = deselect;
    this.isSelected = isSelected;
    this.show = show;
    this.hide = hide;

    // Inherits from Leaflet's default GeoJSON object
    L.GeoJSON.apply(this, [ geojson, STYLE ]);
  };
  SelectableGeometry.prototype = Object.create(L.GeoJSON.prototype);

  return SelectableGeometry;

});
