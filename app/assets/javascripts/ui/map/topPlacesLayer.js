define([
  'ui/common/hasEvents',
  'ui/map/animatedMarker',
  'ui/map/styles'
], function(HasEvents, AnimatedMarker, Styles) {

  // TODO does it make sense to have a common Layer parent class?

  var TopPlacesLayer = function(map) {

    var markers = L.featureGroup().addTo(map),

        // We only support single selection for now
        currentSelection = false,

        clearLayer = function() {
          markers.clearLayers();
        },

        clearSelection = function() {
          if (currentSelection)
            currentSelection.deselect();
        },

        onMarkerClicked = function(e) {
          var marker = e.target,
              place = marker.place,
              isSelected = marker.isSelected();

          console.log(place, { selected: isSelected });

          if (isSelected) {
            clearSelection();
            currentSelection = marker;
          } else {
            clearSelection();
            currentSelection = false;
          }
        },

        createMarker = function(place) {

          // TODO for testing only!
          var firstRecord = place.is_conflation_of[0],
              pt = (firstRecord.representative_point) ? firstRecord.representative_point : false;

          if (pt) {
            marker = new AnimatedMarker([ pt[1], pt[0] ]).addTo(markers);
            marker.on('click', onMarkerClicked);
            marker.place = place;
          }

        },

        update = function(topPlaces) {
          clearLayer();
          topPlaces.forEach(createMarker);
        };

    this.update = update;

    HasEvents.apply(this);
  };
  TopPlacesLayer.prototype = Object.create(HasEvents.prototype);

  return TopPlacesLayer;

});
