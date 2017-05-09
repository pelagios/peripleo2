define([
  'ui/common/hasEvents',
  'ui/map/animatedMarker',
  'ui/map/styles'
], function(HasEvents, AnimatedMarker, Styles) {

  // TODO does it make sense to have a common Layer parent class?

  var TopPlacesLayer = function(map) {

    var self = this,

        markers = L.featureGroup().addTo(map),

        currentSelection = false, // We only support single selection for now

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

          if (isSelected) {
            clearSelection();
            currentSelection = marker;
            self.fireEvent('select', place);
          } else if (currentSelection) {
            clearSelection();
            currentSelection = false;
            self.fireEvent('select');
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
