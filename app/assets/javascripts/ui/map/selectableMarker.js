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

        selection = false,

        select = function() {
          var icon = L.divIcon({
                iconSize  : [ 50, 50 ],
                iconAnchor: [ 25, 25 ],
                className : 'marker-selected',
                html      : '<div class="inner"></div>'
              });

          selection = L.marker(latlng, { icon: icon });
          selection.on('add', function() {
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
        };

    this.isSelected = function() { return selection !== false; };
    this.select = select;
    this.deselect = deselect;

    L.CircleMarker.apply(this, [ latlng, style ]);
  };
  SelectableMarker.prototype = Object.create(L.CircleMarker.prototype);

  return SelectableMarker;

});
