define(['ui/map/styles'], function(Styles) {

  var SELECTION_STYLE = {
        stroke      : false,
        fillOpacity : 0.4,
        fillColor   : '#e75444',
        radius      : 25
      };

  var SelectableMarker = function(latlng, size) {

    var self = this,

        style = jQuery.extend({}, Styles.POINT.RED, { radius: size }),

        selection = false,

        select = function() {
          selection = L.circleMarker(latlng, SELECTION_STYLE).addTo(self._map).bringToBack();
        },

        deselect = function() {
          if (selection) selection.remove();
          selection = false;
        };

    this.isSelected = function() { return selection !== false; };
    this.select = select;
    this.deselect = deselect;

    L.CircleMarker.apply(this, [ latlng, style ]);
  };
  SelectableMarker.prototype = Object.create(L.CircleMarker.prototype);

  return SelectableMarker;

});
