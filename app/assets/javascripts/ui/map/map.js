define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/itemLayer',
  'ui/map/layerSwitcher',
  'ui/map/topPlacesLayer'
], function(HasEvents, ItemUtils, ItemLayer, LayerSwitcher, TopPlacesLayer) {

  // TODO can we make these configurable? Cf. E-ARK demo (where we used a JSON file)
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
            '<div class="control layers icon" title="Change base layer">&#xf0c9;</div>' +
            '<div class="zoom">' +
              '<div class="control zoom-in" title="Zoom in">+</div>' +
              '<div class="control zoom-out" title="Zoom out">&ndash;</div>' +
            '</div>' +
          '</div>').appendTo(document.body),

        itemLayer = new ItemLayer(map),

        topPlacesLayer = new TopPlacesLayer(map),

        layerSwitcher = new LayerSwitcher(),

        btnLayers  = controlsEl.find('.layers'),
        btnZoomIn  = controlsEl.find('.zoom-in'),
        btnZoomOut = controlsEl.find('.zoom-out'),

        fitBounds = function() {
          var topPlacesBounds = topPlacesLayer.getBounds();

          // TODO get item bounds + compute union
          if (topPlacesBounds.isValid())
            map.fitBounds(topPlacesBounds, {
              paddingTopLeft: [440, 20],
              paddingBottomRight: [20, 20],
              animate: true
            });
        },

        onChangeLayer = function(name) {
          var layer = BASE_LAYERS[name];
          if (layer && layer !== currentBaseLayer) {
            map.addLayer(layer);
            map.removeLayer(currentBaseLayer);
            currentBaseLayer = layer;
          }
        },

        onMove = function() {
          var bounds = map.getBounds();
          self.fireEvent('move', [
            bounds.getWest(),
            bounds.getEast(),
            bounds.getSouth(),
            bounds.getNorth()
          ]);
        },

        // Conveniently, this means a click on the base map, not a marker - deselect!
        onClick = function(e) {
          topPlacesLayer.clearSelection();
          self.fireEvent('selectPlace');
        },

        setSearchResponse = function(searchResponse) {
          itemLayer.update(searchResponse.items);
          if (searchResponse.top_places)
            topPlacesLayer.update(searchResponse.top_places);
        },

        setState = function(state) {

        },

        setSelectedItem = function(item, placeReferences) {
          // The item won't necessarily have place references
          var placeURIs = (placeReferences) ?
                placeReferences.map(function(ref) { return ref.identifiers[0]; }) :
                [];

          // TODO rethink all possible situtations
          // TODO - what if the places are not in the topPlaceLayer yet (happens for autosuggest selections!)
          // TODO - the item may have geometry itself

          topPlacesLayer.selectByURIs(placeURIs);
        };

    layerSwitcher.on('changeLayer', onChangeLayer);

    btnLayers.click(function() { layerSwitcher.open(); });
    btnZoomIn.click(function() { map.zoomIn(); });
    btnZoomOut.click(function() { map.zoomOut(); });

    map.on('move', onMove);
    map.on('click', onClick);

    // Forward selections up the hierarchy chain
    topPlacesLayer.on('select', this.forwardEvent('selectPlace'));

    this.fitBounds = fitBounds;
    this.setState = setState;
    this.setSearchResponse = setSearchResponse;
    this.setSelectedItem = setSelectedItem;

    HasEvents.apply(this);
  };
  Map.prototype = Object.create(HasEvents.prototype);

  return Map;

});
