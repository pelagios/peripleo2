define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/selectableMarker'
], function(HasEvents, ItemUtils, SelectableMarker) {

  var MAX_MARKER_SIZE  = 11,

      MIN_MARKER_SIZE = 4;

  var GeometryLayer = function(map) {

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
            var count = place.related_count;
            if (count < min)
              min = count;
            if (count > max)
              max = count;
          });

          if (min === max) {
            // All places are equal (or just one place) - use min marker size
            markerScaleFn = function() { return MIN_MARKER_SIZE; };
          } else {
            // Marker size y = fn(related_count) is linear fn according to y = k * x + d
            k = (MAX_MARKER_SIZE - MIN_MARKER_SIZE) / (max - min);
            d = ((MIN_MARKER_SIZE * max) - (MAX_MARKER_SIZE * min)) / (max - min);
            markerScaleFn = function(relCount) { return k * relCount + d; };
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
          currentSelection = [];
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
              place = marker.place;

          if (!marker.isSelected()) {
            clearSelection();
            marker.select();
            currentSelection = [ marker ];
            self.fireEvent('select', place);
          }

          L.DomEvent.stop(e);
        },

        createMarker = function(place) {
          var pt = (place.representative_point) ? place.representative_point : false,
              uris = ItemUtils.getURIs(place),
              latlng, size, marker;

          if (pt) {
            latlng = [ pt[1], pt[0] ];
            size = markerScaleFn(place.related_count);
            marker = new SelectableMarker(latlng, size).addTo(markers);
            marker.on('click', onMarkerClicked);
            marker.place = place;
          }

          uris.forEach(function(uri) {
            markerIndex[uri] = marker;
          });
        },

        /**
         * Merges the geometries of the search results and top referenced places
         *
         * TODO properly support non-place items with geometry
         */
        mergeGeometries = function(results) {
          var topPlaces = results.top_referenced.PLACE,

              itemsWithGeometry = results.items.filter(function(item) {
                // Skips all items without geometries
                return item.representative_point;
              }),

              // Shorthand so we can quickly test which places exist in topPlaces
              topPlaceIds = (topPlaces) ? topPlaces.map(function(p) { return p.doc_id; }) : [],

              // Clone topPlaces
              merged = (topPlaces) ?
                topPlaces.map(function(p) { return jQuery.extend(true, {}, p); }) : [];

          itemsWithGeometry.forEach(function(item) {
            var existsIdx = topPlaceIds.indexOf(item.doc_id);
            if (existsIdx < 0)
              // Item is not already in topPlaces - add to end of array
              merged.push(jQuery.extend(true, {},  item, { related_count: 0 }));
            else
              // Item is in topPlaces already - increment result_count
              merged[existsIdx].related_count += 1;
          });

          return merged;
        },

        update = function(results) {
          // TODO change this
          var hasPlaceFilter = (results.request_args.filters) ?
                results.request_args.filters.referencing : false;

          // Don't update the map if there's a place filter
          if (!hasPlaceFilter) {
            var placesWithCounts = mergeGeometries(results);
            computeMarkerScaleFn(placesWithCounts);
            clearLayer();
            placesWithCounts.forEach(createMarker);
          }
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
  GeometryLayer.prototype = Object.create(HasEvents.prototype);

  return GeometryLayer;

});
