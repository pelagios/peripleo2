define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/geomUtils',
  'ui/map/selectableGeometry',
  'ui/map/selectableMarker'
], function(HasEvents, ItemUtils, GeomUtils, SelectableGeometry, SelectableMarker) {

  var MARKER_SIZE  = { MIN : 4, MAX: 11 },

      OCCLUSION_THRESHOLD = 0.95;

  var GeometryLayer = function(map) {

    var self = this,

        shapes = L.featureGroup().addTo(map),

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

        /** Removes selection and all markers **/
        clear = function() {
          clearSelection();
          shapes.clearLayers();
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
          var markerBounds = markers.getBounds(),
              markerBoundsValid = markerBounds.isValid(),
              shapeBounds = shapes.getBounds(),
              shapeBoundsValid = shapeBounds.isValid(),
              mergedBounds;

          if (markerBoundsValid && shapeBoundsValid) {
            markerBounds.extend(shapeBounds);
            return markerBounds;
          } else if (markerBoundsValid) {
            return markerBounds;
          } else if (shapeBoundsValid) {
            return shapeBounds;
          } else {
            // Doesn't matter as long as we return invalid bounds
            return markerBounds;
          }
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
          // Use complex (poly, multipoly, linestring) geometry if available
          var poly_geom =
                (place.representative_geometry && place.representative_geometry.type !== 'Point') ?
                  place.representative_geometry : false,

              pt   = (place.representative_point) ? place.representative_point : false,

              uris = ItemUtils.getURIs(place),

              marker = (function() {
                if (poly_geom) {
                  return new SelectableGeometry(poly_geom, shapes).addTo(shapes);
                } else if (pt) {
                  // Place might be there as a direct result, or as a place referenced by a result
                  var refCount = (place.referenced_count) ? place.referenced_count.total : 1,
                      size = markerScaleFn(refCount);

                  return new SelectableMarker([ pt[1], pt[0] ], size).addTo(markers);
                }
              })();

          if (marker) {
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
          markers.bringToFront();
        },

        highlightItems = function(items) {
          var itemsWithGeometry = items.filter(function(i) { return i.representative_point; }),

              // Look up all items in the index, to see which ones are on the map already
              indexLookupResult = itemsWithGeometry.map(function(i) {
                var marker = markerIndex[i.is_conflation_of[0].identifiers[0]];
                return { item: i, marker: marker };
              }),

              // Highlight all items as required, don't re-highlight those that are selected already
              highlighted = indexLookupResult.map(function(r) {
                var marker = r.marker || createMarker(r.item);
                if (!marker.isSelected()) {
                  marker.select();
                  currentSelection.push(marker);
                }
                return marker;
              });

          // Deselect all currently selected items that are not highlighted in this method call
          currentSelection = currentSelection.filter(function(marker) {
            var keepHighlighted = items.indexOf(marker.place) > -1;
            if (keepHighlighted) {
              return true;
            } else {
              marker.deselect();
              return false;
            }
          });
        },

        /** Hides shapes that cover the entire viewport **/
        hideOccludingShapes = function() {
          var visibleShapes = GeomUtils.getVisibleShapes(map, shapes),
              mapBounds = map.getBounds(),
              mapSize = GeomUtils.getSize(mapBounds);

          // Compute degree of overlap
          visibleShapes.forEach(function(shape) {
            var bounds = shape.getBounds(),
                intersection = GeomUtils.intersectBounds(mapBounds, bounds),
                overlap = GeomUtils.getSize(intersection) / mapSize;

            if (overlap > OCCLUSION_THRESHOLD)
              shape.hide();
            else
              shape.show();
          });
        };

    map.on('moveend', hideOccludingShapes);

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
