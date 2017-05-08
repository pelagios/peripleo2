define([
  'ui/common/hasEvents',
  'ui/map/styles'
], function(HasEvents, Styles) {

  var MARKER_SIZE = 4,

      MARKER_GLOW_RADIUS = 15,

      MARKER_BUFFER = MARKER_GLOW_RADIUS / 3,

      MARKER_CANVAS_SIZE = 2 * (MARKER_GLOW_RADIUS + MARKER_BUFFER),

      MARKER_GLOW_BLURRED = false,

      MARKER_SVG = (function() {
        var r = MARKER_GLOW_RADIUS + MARKER_BUFFER,
            w = 2 * r, // Shorthand
            s = MARKER_SIZE, // Shorthand
            svg;

        svg = '<svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="' + w + '" height="' + w + '">';

        if (MARKER_GLOW_BLURRED)
          svg +=
            '<defs>' +
              '<filter id="blur" x="-50%" y="-50%" width="200%" height="200%">' +
                '<feGaussianBlur in="SourceGraphic" stdDeviation="2" />' +
              '</filter>' +
            '</defs>';

        svg +=
          '<circle cx="' + r + '" cy="' + r + '" r="' + MARKER_GLOW_RADIUS + '" fill="rgba(255, 0, 0, 0.5)" ';

        if (MARKER_GLOW_BLURRED)
          svg += 'filter="url(#blur)">';
        else
          svg += '>';

        svg +=
          '<animate attributeName="r" begin="0s" dur="0.4s" repeatCount="1" from="0" to="' + MARKER_GLOW_RADIUS  +
          '"></animate></circle>' +
          '<path class="leaflet-interactive" stroke="#330000" stroke-opacity="1" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" '+
            'fill="#e75444" fill-opacity="1" fill-rule="evenodd" d="M16.5,20a' + s + ',' + s +' 0 1,0 '+ (2 * s) +
            ',0 a' + s + ',' + s + ' 0 1,0 -' + (2 * s) + ',0 "></path>' +
          '</svg>';

        return 'data:image/svg+xml;base64,' + btoa(svg);
      })();

  var myIcon = L.icon({
    iconUrl: MARKER_SVG,
    iconSize: [ MARKER_CANVAS_SIZE, MARKER_CANVAS_SIZE ],
    iconAnchor: [ MARKER_CANVAS_SIZE / 2, MARKER_CANVAS_SIZE / 2 ],
    popupAnchor: [ MARKER_CANVAS_SIZE / 2, MARKER_CANVAS_SIZE / 2 ]
  });

  // TODO does it make sense to have a common Layer parent class?

  var TopPlacesLayer = function(map) {

    var markers = L.featureGroup().addTo(map),

        clear = function() {
          markers.clearLayers();
        },

        createMarker = function(place) {

          // TODO for testing only!
          var firstRecord = place.is_conflation_of[0],
              pt = (firstRecord.representative_point) ? firstRecord.representative_point : false;

          if (pt) {
            // L.marker( [ pt[1], pt[0] ], { icon: myIcon } ).addTo(markers);
            L.circleMarker([ pt[1], pt[0] ], Styles.POINT.RED).addTo(markers);
          }
        },

        update = function(topPlaces) {
          // console.log(topPlaces);
          clear();
          topPlaces.forEach(createMarker);
        };

    this.update = update;

    HasEvents.apply(this);
  };
  TopPlacesLayer.prototype = Object.create(HasEvents.prototype);

  return TopPlacesLayer;

});
