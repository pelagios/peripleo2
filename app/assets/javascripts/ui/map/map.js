define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/baselayers',
  'ui/map/baselayerSwitcher',
  'ui/map/geometryLayer'
], function(HasEvents, ItemUtils, BaseLayers, BaseLayerSwitcher, GeometryLayer) {

  /** A helper that creates a hash of TileLayer objects for all base layers **/
  var LAYERS = (function() {
    var layers = {};
    BaseLayers.all().forEach(function(layer) {
      layers[layer.id] = L.tileLayer(layer.tile_url, {
        attribution : layer.attribution,
        minZoom     : layer.min_zoom,
        maxZoom     : layer.max_zoom
      });
    });
    return layers;
  })();

  var Map = function(containerDiv) {

    var self = this,

        currentBaseLayer = LAYERS.AWMC,

        map = L.map(containerDiv, {
          center: [ 48, 16 ],
          zoom: 4,
          zoomControl: false,
          layers: [ currentBaseLayer ]
        }),

        controls = jQuery(
          '<div id="map-controls">' +
            '<div class="control filter-by-viewport"><span class="icon">&#xf0b0;</span></div>' +
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

        geometryLayer = new GeometryLayer(map),

        baseLayerSwitcher = new BaseLayerSwitcher(),

        // Flag so we can tell apart user-initiated zoom/pan from automatic fit movement
        isAutoFit = false,

        fitBounds = function() {
          var bounds = geometryLayer.getBounds(),
              isPoint = bounds.getSouthWest().equals(bounds.getNorthEast());

          isAutoFit = true;
          if (isPoint)
            map.panTo(bounds.getSouthWest());
          else
            map.fitBounds(bounds, {
              paddingTopLeft: [440, 20],
              paddingBottomRight: [20, 20],
              animate: true
            });
        },

        onChangeBasemap = function(name) {
          var layer = LAYERS[name];
          if (layer && layer !== currentBaseLayer) {
            map.addLayer(layer);
            map.removeLayer(currentBaseLayer);

            currentBaseLayer = layer;

            self.fireEvent('changeBasemap', name);
          }
        },

        onMoved = function(e, f) {
          if (!isAutoFit) {
            var bounds = map.getBounds();
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
          // TODO called on load and Back button - might have effect on map position
        },

        /** Highlights the item along with the referenced places **/
        setSelectedItem = function(item, referencedPlaces) {
          var refs = referencedPlaces || [];
          geometryLayer.highlightItems([ item ].concat(refs));
        };

    baseLayerSwitcher.on('changeBasemap', onChangeBasemap);

    btnFilterByView.click(onToggleFilterByView);
    btnLayers.click(function() { baseLayerSwitcher.open(); });
    btnZoomIn.click(function() { map.zoomIn(); });
    btnZoomOut.click(function() { map.zoomOut(); });

    map.on('moveend', onMoved);
    map.on('click', onClick);

    // Forward selections up the hierarchy chain
    geometryLayer.on('select', this.forwardEvent('selectPlace'));

    this.fitBounds = fitBounds;
    this.setState = setState;
    this.setSearchResponse = geometryLayer.setSearchResponse;
    this.setSelectedItem = setSelectedItem;
    this.clear = geometryLayer.clear;

    HasEvents.apply(this);
  };
  Map.prototype = Object.create(HasEvents.prototype);

  return Map;

});
