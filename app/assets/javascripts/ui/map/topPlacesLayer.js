define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/animatedMarker',
  'ui/map/styles'
], function(HasEvents, ItemUtils, AnimatedMarker, Styles) {

  // TODO does it make sense to have a common Layer parent class?

  var TopPlacesLayer = function(map) {

    var self = this,

        markers = L.featureGroup().addTo(map),

        markerIndex = {},

        // A list of AnimatedMarkers
        currentSelection = [],

        clearLayer = function() {
          markerIndex = {};
          markers.clearLayers();
        },

        clearSelection = function() {
          currentSelection.forEach(function(marker) {
            marker.deselect();
          });
        },

        isEqualToCurrentSelection = function(markers) {
          if (markers.length !== currentSelection.length)
            return false;

          return markers.filter(function(m) {
            return currentSelection.indexOf(m) < 0;
          }).length === 0;
        },

        onMarkerClicked = function(e) {
          var marker = e.target,
              place = marker.place,
              isSelected = marker.isSelected();

          if (isSelected) {
            clearSelection();
            currentSelection = [ marker ];
            self.fireEvent('select', place);
          } else {
            clearSelection();
            currentSelection = [];
            self.fireEvent('select');
          }
        },

        createMarker = function(place) {

          // TODO for testing only!
          var firstRecord = place.is_conflation_of[0],
              pt = (firstRecord.representative_point) ? firstRecord.representative_point : false,
              uris = ItemUtils.getURIs(place),
              marker;

          if (pt) {
            marker = new AnimatedMarker([ pt[1], pt[0] ]).addTo(markers);
            marker.on('click', onMarkerClicked);
            marker.place = place;
          }

          uris.forEach(function(uri) {
            markerIndex[uri] = marker;
          });
        },

        update = function(topPlaces) {
          clearLayer();
          topPlaces.forEach(createMarker);
        },

        selectByURIs = function(uris) {
          var selection = uris.map(function(uri) {
                return markerIndex[uri];
              }).filter(function(n) { return n !== undefined; });

          if (!isEqualToCurrentSelection(selection)) {
            clearSelection();
            currentSelection = selection;

            selection.forEach(function(marker) {
              marker.select();
            });
          }
        };

    this.update = update;
    this.selectByURIs = selectByURIs;

    HasEvents.apply(this);
  };
  TopPlacesLayer.prototype = Object.create(HasEvents.prototype);

  return TopPlacesLayer;

});
