require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/common/map/baselayers',
  'ui/controls/selection/depiction/iiifView',
  'ui/map/selectableMarker'
], function(BaseLayers, IIIFView, Marker) {

  jQuery(document).ready(function() {
    var hasIIIF = jQuery('.iiif').length > 0,
        hasMap = jQuery('.map').length > 0,

        initIIIF = function() {
          var container = jQuery('.iiif'),
              url = container.data('url'),
              source = container.data('source'),
              iiifView = new IIIFView(container, { url: url, source: source });
        },

        initMap = function() {
          var containerDiv = jQuery('.map'),
              lat = parseFloat(containerDiv.data('lat')),
              lng = parseFloat(containerDiv.data('lng')),
              baseMap = BaseLayers.getLayer('AWMC'),

              tileLayer = L.tileLayer(baseMap.tile_url, {
                attribution : baseMap.attribution,
                minZoom     : baseMap.min_zoom,
                maxZoom     : baseMap.max_zoom
              }),

              map = L.map(containerDiv[0], {
                center: [ lat, lng ],
                zoom: 4,
                zoomControl: false,
                layers: [ tileLayer ]
              }),

              marker = new Marker([lat, lng], 4).addTo(map);

          marker.select();
        };

    if (hasIIIF) initIIIF();
    if (hasMap) initMap();

  });

});
