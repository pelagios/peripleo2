define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/animatedMarker',
  'ui/map/styles'
], function(HasEvents, ItemUtils, AnimatedMarker, Styles) {

  var MAX_MARKER_SIZE  = 11,

      MIN_MARKER_SIZE = 4;

  var TopPlacesLayer = function(map) {

    var self = this,

        markers = L.featureGroup().addTo(map),

        markerIndex = {},

        markerScaleFn,

        // A list of AnimatedMarkers
        currentSelection = [],

        computeMarkerScaleFn = function(places) {
          var min = 9007199254740991, max = 1,
              k, d, avg;

          // Determine min/max results per marker
          places.forEach(function(place) {
            var count = place.result_count;
            if (count < min)
              min = count;
            if (count > max)
              max = count;
          });

          if (min === max) {
            // All places are equal (or just one place) - use min marker size
            markerScaleFn = function() { return MIN_MARKER_SIZE; };
          } else {
            // Marker size y = fn(result_count) is linear fn according to y = k * x + d
            k = (MAX_MARKER_SIZE - MIN_MARKER_SIZE) / (max - min);
            d = ((MIN_MARKER_SIZE * max) - (MAX_MARKER_SIZE * min)) / (max - min);
            markerScaleFn = function(resultCount) { return k * resultCount + d; };
          }
        },

        clearLayer = function() {
          markerIndex = {};
          markers.clearLayers();
        },

        clearSelection = function() {
          currentSelection.forEach(function(marker) {
            marker.deselect();
          });
        },

        getBounds = function() {
          return markers.getBounds();
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
          var pt = (place.representative_point) ? place.representative_point : false,
              uris = ItemUtils.getURIs(place),
              latlng, size, marker;

          if (pt) {
            latlng = [ pt[1], pt[0] ];
            size = markerScaleFn(place.result_count);
            marker = new AnimatedMarker(latlng, size).addTo(markers);
            marker.on('click', onMarkerClicked);
            marker.place = place;
          }

          uris.forEach(function(uri) {
            markerIndex[uri] = marker;
          });
        },

        update = function(topPlaces) {
          computeMarkerScaleFn(topPlaces);
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

    this.clearSelection = clearSelection;
    this.getBounds = getBounds;
    this.update = update;
    this.selectByURIs = selectByURIs;

    HasEvents.apply(this);
  };
  TopPlacesLayer.prototype = Object.create(HasEvents.prototype);

  return TopPlacesLayer;

});
