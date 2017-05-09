define([
  'ui/common/hasEvents',
  'ui/map/itemLayer',
  'ui/map/topPlacesLayer'
], function(HasEvents, ItemLayer, TopPlacesLayer) {

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

        itemLayer = new ItemLayer(map),

        topPlacesLayer = new TopPlacesLayer(map),

        setResponse = function(searchResponse) {
          itemLayer.update(searchResponse.items);
          if (searchResponse.top_places)
            topPlacesLayer.update(searchResponse.top_places);
        },

        setState = function(state) {

        };

    // Forward selections up the hierarchy chain
    topPlacesLayer.on('select', this.forwardEvent('selectPlace'));

    this.setResponse = setResponse;
    this.setState = setState;

    HasEvents.apply(this);
  };
  Map.prototype = Object.create(HasEvents.prototype);

  return Map;

});
