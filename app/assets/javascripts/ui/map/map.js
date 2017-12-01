define([
  'ui/common/itemUtils',
  'ui/common/map/mapBase',
  'ui/map/geometryLayer'
], function(ItemUtils, MapBase, GeometryLayer) {

  var Map = function(containerDiv) {

    var self = this,

        controls = jQuery(
          '<div class="map-controls">' +
            '<a href="/" class="control home" title="Learn more"><span class="icon">&#xf015;</span></a>' +
            '<div class="control filter-by-viewport" title="Filter by viewport"><span class="icon">&#xf0b0;</span></div>' +
            '<div class="control layers icon" title="Change base layer">&#xf0c9;</div>' +
            '<div class="zoom">' +
              '<div class="control zoom-in" title="Zoom in">+</div>' +
              '<div class="control zoom-out" title="Zoom out">&ndash;</div>' +
            '</div>' +
          '</div>').appendTo(document.body),

        btnFilterByView = controls.find('.filter-by-viewport'),
        btnLayers       = controls.find('.layers'),
        btnZoomIn       = controls.find('.zoom-in'),
        btnZoomOut      = controls.find('.zoom-out'),

        // To be initialized after the MapBase superclass is built
        geometryLayer,

        // Flag so we can tell apart user-initiated zoom/pan from automatic fit movement
        isAutoFit = false,

        fitBounds = function() {
          var bounds = geometryLayer.getBounds(),

              opts = {
                paddingTopLeft: [440, 20],
                paddingBottomRight: [20, 20],
                animate: true
              };

          if (bounds.isValid()) {
            isAutoFit = true;
            self.fit(bounds, opts);
          }
        },

        onMoved = function(e, f) {
          if (!isAutoFit) {
            var bounds = self.map.getBounds();
            self.fireEvent('move', [
              bounds.getWest(),
              bounds.getEast(),
              bounds.getSouth(),
              bounds.getNorth()
            ]);
          } else {
            // Reset autofit flag
            isAutoFit = false;
          }
        },

        /**
         * Note: we're stopping event propagation on markers, therefore we
         * know this click is coming from the basemap - deselect!
         */
        onClick = function(e) {
          geometryLayer.clearSelection();
          self.fireEvent('selectPlace');
        },

        /** Toggles the 'filter by view' button **/
        onToggleFilterByView = function() {
          var isEnabled = btnFilterByView.hasClass('enabled');
          if (isEnabled)
            btnFilterByView.removeClass('enabled');
          else
            btnFilterByView.addClass('enabled');

          self.fireEvent('filterByViewport', !isEnabled);
        },

        setState = function(state) {
          // TODO called on load and Back button - might have effect on map position in the future
        },

        /** Highlights the item along with the referenced places **/
        setSelectedItem = function(item, referencedPlaces) {
          var refs = referencedPlaces || [];
          geometryLayer.highlightItems([ item ].concat(refs));
        };

    MapBase.apply(this, [ containerDiv ]);

    geometryLayer = new GeometryLayer(self.map);

    btnFilterByView.click(onToggleFilterByView);
    btnLayers.click(function() { self.selectBasemap(); });
    btnZoomIn.click(function() { self.map.zoomIn(); });
    btnZoomOut.click(function() { self.map.zoomOut(); });

    this.map.on('moveend', onMoved);
    this.map.on('click', onClick);

    // Forward selections up the hierarchy chain
    geometryLayer.on('select', this.forwardEvent('selectPlace'));

    this.fitBounds = fitBounds;
    this.setState = setState;
    this.setSearchResponse = geometryLayer.setSearchResponse;
    this.setSelectedItem = setSelectedItem;
    this.clear = geometryLayer.clear;
  };
  Map.prototype = Object.create(MapBase.prototype);

  return Map;

});
