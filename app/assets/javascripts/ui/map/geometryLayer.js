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

        /** Function to compute marker size from no. of referencing items **/
        markerScaleFn = function() { return MARKER_SIZE.MIN; },

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
        clear = function() {
          markerIndex = {};
          markers.clearLayers();
          markerScaleFn = function() { return MARKER_SIZE.MIN; };
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
          var pt   = (place.representative_point) ? place.representative_point : false;

          if (pt) {
            var uris = ItemUtils.getURIs(place),
                latlng = [ pt[1], pt[0] ],

                // Place might be there as a direct result, or as a place referenced by a result
                refCount = (place.referenced_count) ? place.referenced_count.total : 1,
                size = markerScaleFn(refCount),
                marker = new SelectableMarker(latlng, size).addTo(markers);

            marker.on('click', onMarkerClicked);
            marker.place = place;

            uris.forEach(function(uri) {
              markerIndex[uri] = marker;
            });

            return marker;
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
            // else - // TODO we probably shouldn't increase the ref count - keep things clean!
              // Item is in topPlaces already (it's a place) - increment result_count
              // merged[existsIdx].referenced_count.total += 1;
          });

          return merged;
        },

        setSearchResponse = function(results) {
          var placesWithCounts = mergeGeometries(results);
          clear();
          computeMarkerScaleFn(placesWithCounts);
          placesWithCounts.forEach(createMarker);
        },

        highlightItems = function(items) {
          var itemsWithGeometry = items.filter(function(i) { return i.representative_point; }),

              // Look up all items in the index, to see which ones are on the map already
              indexLookupResult = itemsWithGeometry.map(function(i) {
                var marker = markerIndex[i.is_conflation_of[0].identifiers[0]];
                return { item: i, marker: marker };
              });

          // TODO don't re-select markers that are already selected
          clearSelection();

          indexLookupResult.forEach(function(t) {
            var marker = t.marker || createMarker(t.item);
            marker.select();
            currentSelection.push(marker);
          });
        };

    this.getBounds = getBounds;
    this.clear = clear;
    this.clearSelection = clearSelection;
    this.setSearchResponse = setSearchResponse;
    this.highlightItems = highlightItems;

    HasEvents.apply(this);
  };
  GeometryLayer.prototype = Object.create(HasEvents.prototype);

  return GeometryLayer;

});
