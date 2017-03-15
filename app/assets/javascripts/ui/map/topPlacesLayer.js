define([
  'ui/common/hasEvents',
  'ui/map/styles'
], function(HasEvents, Styles) {

  // TODO does it make sense to have a common Layer parent class?

  var TopPlacesLayer = function(map) {

    var markers = L.featureGroup().addTo(map),

        clear = function() {
          markers.clearLayers();
        },

        createMarker = function(place) {

          // TODO for testing only!
          var firstRecord = place.is_conflation_of[0],
              pt = (firstRecord.representative_point) ? firstRecord.representative_point : false;

          if (pt)
            L.circleMarker([ pt[1], pt[0] ], Styles.POINT.RED).addTo(markers);
        },

        update = function(topPlaces) {
          // console.log(topPlaces);
          clear();
          topPlaces.forEach(createMarker);
        };

    this.update = update;

    HasEvents.apply(this);
  };
  TopPlacesLayer.prototype = Object.create(HasEvents.prototype);

  return TopPlacesLayer;

});
