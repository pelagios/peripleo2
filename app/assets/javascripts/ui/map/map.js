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

        setSearchResponse = function(searchResponse) {
          itemLayer.update(searchResponse.items);
          if (searchResponse.top_places)
            topPlacesLayer.update(searchResponse.top_places);
        },

        setState = function(state) {

        },

        setSelectedItem = function(item, places) {
          // Various possibilities (more than one can apply)
          // - the item could be linked to one or more of the top places
          //   (in this case we don't know the place - but the selection panel already runs a
          //   a query - refactor so that this happens in the app?)
          // - the item may have geometry itself (at the moment, this isn't shown)
          // - the item might not be on the map at all (direct selection from autosuggest)
          //   in this case (again) a request for (top) places for the item needs to be
          //   made in advance
        };

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
