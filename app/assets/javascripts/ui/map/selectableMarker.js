define([], function() {

  var BASE_STYLE = {
    color       : '#a64a40',
    opacity     : 1,
    fillColor   : '#e75444',
    fillOpacity : 1,
    weight      : 1.5
  };

  var SelectableMarker = function(latlng, size) {

    var self = this,

        style = jQuery.extend({}, BASE_STYLE, { radius: size }),

        /** The selection marker (if any) **/
        selection = false,

        select = function() {
          selection = L.marker(latlng, {
            icon: L.divIcon({
              iconSize  : [ 50, 50 ],
              iconAnchor: [ 25, 25 ],
              className : 'marker-selected',
              html      : '<div class="inner"></div>'
            })
          });

          selection.on('add', function() {
            // Start animation right after add
            setTimeout(function() { jQuery(selection._icon).css('padding', 0); }, 10);
          });

          selection.addTo(self._map);
        },

        deselect = function() {
          if (selection) {
            var marker = selection;
            selection = false;
            jQuery(marker._icon).css('padding', '25px');
            setTimeout(function() { marker.remove(); }, 150);
          }
        },

        isSelected = function() {
          return selection !== false;
        };

    this.select = select;
    this.deselect = deselect;
    this.isSelected = isSelected;

    // Inherits from Leaflet's default circle marker
    L.CircleMarker.apply(this, [ latlng, style ]);
  };
  SelectableMarker.prototype = Object.create(L.CircleMarker.prototype);

  return SelectableMarker;

});
