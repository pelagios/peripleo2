require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/common/map/baselayers',
  'ui/common/itemUtils',
  'ui/controls/selection/depiction/iiifView',
  'ui/map/selectableMarker'
], function(BaseLayers, ItemUtils, IIIFView, Marker) {

  jQuery(document).ready(function() {

    var containerDiv = jQuery('.map'),

        hasIIIF    = jQuery('.iiif').length > 0,
        hasMap     = jQuery('.map').length > 0,
        hasItemIDs = jQuery('.item-identifiers').length > 0,

        identifier = containerDiv.data('id'),

        targetURL = jsRoutes.controllers.ApplicationController.ui().absoluteURL() +
          '#selected=' + encodeURIComponent(identifier),

        isOverflown = (function() {
          var desc = jQuery('.item-description');
          if (desc.length === 0)
            return false;
          else
            return desc[0].scrollHeight > desc[0].clientHeight;
        })(),

        initIIIF = function() {
          var container = jQuery('.iiif'),
              url = container.data('url'),
              source = container.data('source'),
              iiifView = new IIIFView(container, { url: url, source: source });
        },

        initMap = function() {
          var lat = parseFloat(containerDiv.data('lat')),
              lng = parseFloat(containerDiv.data('lng')),
              baseMap = BaseLayers.getLayer('AWMC'),

              tileLayer = L.tileLayer(baseMap.tile_url, {
                attribution : baseMap.attribution,
                minZoom     : baseMap.min_zoom,
                maxZoom     : baseMap.max_zoom
              }),

              map = L.map(containerDiv[0], {
                center: [ lat, lng ],
                zoom: 3,
                zoomControl: false,
                layers: [ tileLayer ]
              }),

              marker = new Marker([lat, lng], 4).addTo(map);

          marker.select();
          map.on('click', function() { window.open(targetURL, '_blank'); });
        },

        initItemIDs = function() {
          var list = jQuery('.item-identifiers li');
          jQuery.each(list, function(idx, el) {
            var li = jQuery(el),
                uri = li.data('uri'),
                parsed = ItemUtils.parseEntityURI(uri);

            if (parsed.shortcode) {
              li.css('backgroundColor', parsed.color);
              li.html('<a href="' + uri + '" target="_blank">' + parsed.shortcode + ':' + parsed.id + '</a>');
            } else {
              li.html('<a href="' + uri + '" target="_blank">' + uri + '<a>');
            }
          });
        },

        addOverflowHint = function() {
          var description = jQuery('.item-description');
          jQuery(
            '<div class="overflow-hint">' +
              '<a href="' + targetURL + '" target="_blank">More...</a>' +
            '</div>').appendTo(description);
        };

    if (hasIIIF) initIIIF();
    if (hasMap) initMap();
    if (hasItemIDs) initItemIDs();
    if (isOverflown) addOverflowHint();
  });

});
