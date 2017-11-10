define([
  'ui/common/hasEvents',
  'ui/common/map/baselayers',
  'ui/common/map/baselayerSwitcher'
], function(HasEvents, BaseLayers, BaseLayerSwitcher) {

  var MapBase = function(containerDiv) {

    var self = this,

        // Creates a hash of TileLayer objects for all base layers
        LAYERS = (function() {
          var layers = {};
          BaseLayers.all().forEach(function(layer) {
            layers[layer.id] = L.tileLayer(layer.tile_url, {
              attribution : layer.attribution,
              minZoom     : layer.min_zoom,
              maxZoom     : layer.max_zoom
            });
          });
          return layers;
        })(),

        currentBaseLayer = LAYERS.AWMC,

        map = L.map(containerDiv, {
          center: [ 48, 16 ],
          zoom: 4,
          zoomControl: false,
          layers: [ currentBaseLayer ]
        }),

        baseLayerSwitcher = new BaseLayerSwitcher(),

        onChangeBasemap = function(name) {          
          var layer = LAYERS[name];
          if (layer && layer !== currentBaseLayer) {
            map.addLayer(layer);
            map.removeLayer(currentBaseLayer);

            currentBaseLayer = layer;

            self.fireEvent('changeBasemap', name);
          }
        },

        selectBasemap = function() {
          baseLayerSwitcher.open();
        };

    baseLayerSwitcher.on('changeBasemap', onChangeBasemap);

    this.selectBasemap = selectBasemap;
    this.map = map;

    HasEvents.apply(this);
  };
  MapBase.prototype = Object.create(HasEvents.prototype);

  return MapBase;

});
