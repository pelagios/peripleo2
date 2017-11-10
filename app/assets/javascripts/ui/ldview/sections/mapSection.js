define([
  'ui/common/map/mapBase'
], function(MapBase) {

  var MapSection = function(containerDiv, item) {

    var self = this;

    MapBase.apply(this, [ containerDiv ]);

    item.is_conflation_of.forEach(function(record) {
      if (record.representative_point)
        L.marker([ record.representative_point[1], record.representative_point[0] ]).addTo(self.map);
    });
  };
  Map.prototype = Object.create(MapBase.prototype);

  return MapSection;

});
