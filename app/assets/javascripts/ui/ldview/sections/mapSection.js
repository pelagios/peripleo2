define([
  'ui/common/map/mapBase'
], function(MapBase) {

  var MapSection = function(containerDiv, item) {

    var self = this,

        btnLayers = jQuery(
          '<div class="map-controls">' +
            '<div class="control layers icon" title="Change base layer">&#xf0c9;</div>' +
          '</div>').appendTo(containerDiv),

        markers,

        createMarker = function(record) {
          return L.marker([ record.representative_point[1], record.representative_point[0] ]).addTo(markers);
        };

    MapBase.apply(this, [ containerDiv ]);

    markers = L.featureGroup().addTo(self.map);
    btnLayers.click(function() { self.selectBasemap(); });

    item.is_conflation_of.forEach(function(record) {
      if (record.representative_point) createMarker(record);
    });
  };
  MapSection.prototype = Object.create(MapBase.prototype);

  return MapSection;

});
