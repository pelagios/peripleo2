define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/map/itemLayer',
  'ui/map/layerSwitcher',
  'ui/map/topPlacesLayer'
], function(HasEvents, ItemUtils, ItemLayer, LayerSwitcher, TopPlacesLayer) {

  // TODO can we make these configurable? Cf. E-ARK demo (where we used a JSON file)
  var BASE_LAYERS = {

        AWMC : L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
                 attribution: 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
                   'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
                   'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                   '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
               })

      };

  var Map = function(containerDiv) {

    var self = this,

        map = L.map(containerDiv, {
          center: [ 48, 16 ],
          zoom: 4,
          zoomControl: false,
          layers: [ BASE_LAYERS.AWMC ]
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

    btnLayers.click(function() { layerSwitcher.open(); });
    btnZoomIn.click(function() { map.zoomIn(); });
    btnZoomOut.click(function() { map.zoomOut(); });

    map.on('click', onClick);

    // Forward selections up the hierarchy chain
    topPlacesLayer.on('select', this.forwardEvent('selectPlace'));

    this.setState = setState;
    this.setSearchResponse = setSearchResponse;
    this.setSelectedItem = setSelectedItem;

    HasEvents.apply(this);
  };
  Map.prototype = Object.create(HasEvents.prototype);

  return Map;

});
