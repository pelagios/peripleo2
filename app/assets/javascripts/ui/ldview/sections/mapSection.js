define([
  'ui/common/itemUtils',
  'ui/common/map/mapBase'
], function(ItemUtils, MapBase) {

  var MARKER_STYLE = {
        radius      : 7,
        stroke      : true,
        color       : '#3f3f3f',
        weight      : 2,
        opacity     : 1,
        fill        : true,
        fillColor   : '#fff',
        fillOpacity : 1
      };

  var MapSection = function(containerDiv, item) {

    var self = this,

        btnLayers = jQuery(
          '<div class="map-controls">' +
            '<div class="control layers icon" title="Change base layer">&#xf0c9;</div>' +
          '</div>').appendTo(containerDiv),

        markers,

        renderMarkers = function() {
          var createMarker = function(record) {
                // TODO support geometry later
                var lat = record.representative_point[1],
                    lon = record.representative_point[0],

                    parsedId = ItemUtils.parseEntityURI(record.uri),

                    style = (parsedId.color) ?
                      jQuery.extend({}, MARKER_STYLE, { fillColor: parsedId.color }) : MARKER_STYLE;

                return L.circleMarker([lat, lon], style).addTo(markers);
              };

          item.is_conflation_of.forEach(function(record) {
            if (record.representative_point) createMarker(record);
          });

          self.fit(markers.getBounds(), {
            padding: [20, 20],
            animate: false
          });
        };

    MapBase.apply(this, [ containerDiv ]);

    markers = L.featureGroup().addTo(self.map);
    btnLayers.click(function() { self.selectBasemap(); });

    renderMarkers();
  };
  MapSection.prototype = Object.create(MapBase.prototype);

  return MapSection;

});
