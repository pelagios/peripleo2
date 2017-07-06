define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/baselayerSwitcher',
  'ui/map/geometryLayer'
], function(HasEvents, ItemUtils, BaseLayerSwitcher, GeometryLayer) {

  // TODO can we make these configurable? Cf. E-ARK demo (where we used a JSON file)
  // TODO move these into BaseLayerSwitcher?
  var BASE_LAYERS = {

        DARE   : L.tileLayer('http://pelagios.org/tilesets/imperium/{z}/{x}/{y}.png', {
                   attribution: 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
                   minZoom:3,
                   maxZoom:11
                 }),

        AWMC   : L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
                   attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                     'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                     'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                     '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
                 }),

        OSM    : L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                   attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
                 }),

        AERIAL : L.tileLayer('http://api.tiles.mapbox.com/v4/mapbox.satellite/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoicGVsYWdpb3MiLCJhIjoiMWRlODMzM2NkZWU3YzkxOGJkMDFiMmFiYjk3NWZkMmUifQ.cyqpSZvhsvBGEBwRfniVrg', {
                   attribution: '<a href="https://www.mapbox.com/about/maps/">&copy; Mapbox</a> <a href="http://www.openstreetmap.org/about/">&copy; OpenStreetMap</a>',
                   maxZoom:22
                 })

      };

  var Map = function(containerDiv) {

    var self = this,

        currentBaseLayer = BASE_LAYERS.AWMC,

        map = L.map(containerDiv, {
          center: [ 48, 16 ],
          zoom: 4,
          zoomControl: false,
          layers: [ currentBaseLayer ]
        }),

        controlsEl = jQuery(
          '<div id="map-controls">' +
            '<div class="control filter-by-viewport"><span class="icon">&#xf0b0;</span></div>' +
            '<div class="control layers icon" title="Change base layer">&#xf0c9;</div>' +
            '<div class="zoom">' +
              '<div class="control zoom-in" title="Zoom in">+</div>' +
              '<div class="control zoom-out" title="Zoom out">&ndash;</div>' +
            '</div>' +
          '</div>').appendTo(document.body),

        geometryLayer = new GeometryLayer(map),

        baseLayerSwitcher = new BaseLayerSwitcher(),

        btnFilterByView = controlsEl.find('.filter-by-viewport'),
        btnLayers       = controlsEl.find('.layers'),
        btnZoomIn       = controlsEl.find('.zoom-in'),
        btnZoomOut      = controlsEl.find('.zoom-out'),

        // Flag so we can tell apart user-initiated zoom/pan from automatic fit movement
        isAutoFit = false,

        fitBounds = function() {
          var topPlacesBounds = geometryLayer.getBounds();

          // TODO get item bounds + compute union
          if (topPlacesBounds.isValid()) {
            isAutoFit = true;

            map.fitBounds(topPlacesBounds, {
              paddingTopLeft: [440, 20],
              paddingBottomRight: [20, 20],
              animate: true
            });
          }
        },

        onChangeLayer = function(name) {
          var layer = BASE_LAYERS[name];
          if (layer && layer !== currentBaseLayer) {
            map.addLayer(layer);
            map.removeLayer(currentBaseLayer);
            currentBaseLayer = layer;
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

        // We're stopping event propagation on markers, so this click is on the basemap - deselect!
        onClick = function(e) {
          geometryLayer.clearSelection();
          self.fireEvent('selectPlace');
        },

        setState = function(state) {

        },

        setSelectedItem = function(item, relatedPlaces) {
          // The item won't necessarily have place references
          var placeURIs = (relatedPlaces) ?
                relatedPlaces.map(function(p) { return p.is_conflation_of[0].identifiers[0]; }) :
                [];

          // TODO rethink all possible situtations
          // TODO - what if the places are not in the topPlaceLayer yet (happens for autosuggest selections!)
          // TODO - the item may have geometry itself

          geometryLayer.selectByURIs(placeURIs);
        },

        onToggleFilterByView = function() {
          var isEnabled = btnFilterByView.hasClass('enabled');
          if (isEnabled)
            btnFilterByView.removeClass('enabled');
          else
            btnFilterByView.addClass('enabled');

          self.fireEvent('filterByViewport', !isEnabled);
        };

    baseLayerSwitcher.on('changeLayer', onChangeLayer);

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
    this.setSearchResponse = geometryLayer.update;
    this.setSelectedItem = setSelectedItem;

    HasEvents.apply(this);
  };
  Map.prototype = Object.create(HasEvents.prototype);

  return Map;

});
