define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/selectableMarker'
], function(HasEvents, ItemUtils, SelectableMarker) {

  var MARKER_SIZE  = { MIN : 4, MAX: 11 };

  var GeometryLayer = function(map) {

    var self = this,

        markers = L.featureGroup().addTo(map),

        /** Index of markers by URI **/
        markerIndex = {},

        markerScaleFn,

        /** List of currently selected markers **/
        currentSelection = [],

        /**
         * Based on a set of places, this function computes a scale
         * function r = fn(x), where x is the number of references at the
         * place, and r is the radius of the marker to be drawn.
         */
        computeMarkerScaleFn = function(places) {
          var min = 9007199254740991, max = 1,
              k, d, avg;

          // Determine min/max results per marker
          places.forEach(function(place) {
            var count = place.referenced_count.total;
            if (count < min)
              min = count;
            if (count > max)
              max = count;
          });

          if (min === max) {
            // All places are equal (or just one place) - use min marker size
            markerScaleFn = function() { return MARKER_SIZE.MIN; };
          } else {
            // Marker size y = fn(referenced_count.total) is linear fn according to y = k * x + d
            k = (MARKER_SIZE.MAX - MARKER_SIZE.MIN) / (max - min);
            d = ((MARKER_SIZE.MIN * max) - (MARKER_SIZE.MAX * min)) / (max - min);
            markerScaleFn = function(refCount) { return k * refCount + d; };
          }
        },

        /** Removes all markers **/
        clearLayer = function() {
          markerIndex = {};
          markers.clearLayers();
        },

        /** Clears the selection (if any) **/
        clearSelection = function() {
          currentSelection.forEach(function(marker) {
            marker.deselect();
          });
          currentSelection = [];
        },

        /** Returns the bounds of the layer **/
        getBounds = function() {
          return markers.getBounds();
        },

        /** On marker click, fire select event **/
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

        /** Creates a marker for the given place **/
        createMarker = function(place) {
          var pt   = (place.representative_point) ? place.representative_point : false,
              uris = ItemUtils.getURIs(place),
              latlng, size, marker;

          if (pt) {
            latlng = [ pt[1], pt[0] ];
            size = markerScaleFn(place.referenced_count.total);

            marker = new SelectableMarker(latlng, size).addTo(markers);
            marker.on('click', onMarkerClicked);
            marker.place = place;

            uris.forEach(function(uri) {
              markerIndex[uri] = marker;
            });
          }
        },

        /** Merges the geometries of the search results and top referenced places **/
        mergeGeometries = function(results) {
          var topPlaces = results.top_referenced.PLACE,

              // Just a shorthand so we can do quicker lookups
              topPlaceIds = (topPlaces) ? topPlaces.map(function(p) { return p.doc_id; }) : [],

              itemsWithGeometry = results.items.filter(function(item) {
                return item.representative_point;
              }),

              // Clone topPlaces - later this will hold the merged list of items
              merged = (topPlaces) ? topPlaces.map(function(p) { return jQuery.extend(true, {}, p); }) : [];

          itemsWithGeometry.forEach(function(item) {
            var existsIdx = topPlaceIds.indexOf(item.doc_id);
            if (existsIdx < 0)
              // Item is not among topPlaces - append it to end of array
              merged.push(jQuery.extend(true, {},  item, { referenced_count : { total: 0 } }));
            else
              // Item is in topPlaces already (it's a place) - increment result_count
              merged[existsIdx].referenced_count.total += 1;
          });

          return merged;
        },

        setSearchResponse = function(results) {
          var placesWithCounts = mergeGeometries(results);
          computeMarkerScaleFn(placesWithCounts);
          clearLayer();
          placesWithCounts.forEach(createMarker);
        },

        selectByURIs = function(uris) {
          var markersToSelect = uris.map(function(uri) {
                return markerIndex[uri];
              }).filter(function(n) { return n !== undefined; }),

              // Just a minimal plausbility check - see below
              isEqualToCurrentSelection = function(markers) {
                if (markers.length !== currentSelection.length)
                  return false;

                return markers.filter(function(m) {
                  return currentSelection.indexOf(m) < 0;
                }).length === 0;
              };

          // Note: this checks equality on the entire selection. Ideally, we may want
          // to compute a diff instead in the future, and then just modify the selection
          // as needed.
          if (!isEqualToCurrentSelection(markersToSelect)) {
            clearSelection();
            markersToSelect.forEach(function(marker) {
              marker.select();
            });

            currentSelection = markersToSelect;
          }
        };

    this.getBounds = getBounds;
    this.clearSelection = clearSelection;
    this.setSearchResponse = setSearchResponse;
    this.selectByURIs = selectByURIs;

    HasEvents.apply(this);
  };
  GeometryLayer.prototype = Object.create(HasEvents.prototype);

  return GeometryLayer;

});
