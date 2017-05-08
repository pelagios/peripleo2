define([], function() {

  var STYLE = {

        DOT_SIZE       : 4,

        STROKE_COLOR   : '#330000',
        STROKE_OPACITY : 1,
        STROKE_WIDTH   : 1.5,

        FILL_COLOR     : '#e75444',
        FILL_OPACITY   : 1,

        GLOW_RADIUS    : 15,
        GLOW_FILL      : 'rgba(255, 0, 0, 0.5)',
        GLOW_BLURRED   : true

      },

      ICON_SIZE = 8 * STYLE.GLOW_RADIUS / 3,

      BASE_SVG = (function() {
        var w = ICON_SIZE / 2,

            s = STYLE.DOT_SIZE, // Shorthand

            svg = jQuery(
              '<svg xmlns="http://www.w3.org/2000/svg" version="1.1">' +
                '<path class="leaflet-interactive" stroke-linecap="round" stroke-linejoin="round" ' +
                  'fill-rule="evenodd"></path>'  +
              '</svg>'),

            path = svg.find('path'),

            x = w - STYLE.DOT_SIZE / 2 - STYLE.STROKE_WIDTH,

            d = 'M' + x + ',' + w + 'a' + s + ',' + s +' 0 1,0 '+ (2 * s) + ',0 a' +
              s + ',' + s + ' 0 1,0 -' + (2 * s) + ',0';

        svg.attr('width', 2 * w);
        svg.attr('height', 2 * w);

        path.attr('stroke', STYLE.STROKE_COLOR);
        path.attr('stroke-opacity', STYLE.STROKE_OPACITY);
        path.attr('stroke-width', STYLE.STROKE_WIDTH);
        path.attr('fill', STYLE.FILL_COLOR);
        path.attr('fill-opacity', STYLE.FILL_OPACITY);
        path.attr('d', d);

        return svg;
      })(),

      SELECTED_SVG = (function() {
        var svg = BASE_SVG.clone(),
            offset = ICON_SIZE / 2,
            glow = jQuery('<circle></circle>');

        glow.attr('cx', offset);
        glow.attr('cy', offset);
        glow.attr('r', STYLE.GLOW_RADIUS);
        glow.attr('fill', STYLE.GLOW_FILL);

        glow.append(
          '<animate attributeName="r" begin="0s" dur="0.4s" repeatCount="1" from="0" ' +
            'to="' + STYLE.GLOW_RADIUS  +'"></animate>');

        svg.prepend(glow);

        if (STYLE.GLOW_BLURRED) {
          // parseXML needed to to ensure case-sensitive handling - otherwise feGaussianBlur
          // goes all lowercase
          svg.prepend(jQuery.parseXML(
            '<defs>' +
              '<filter id="blur" x="-50%" y="-50%" width="200%" height="200%">' +
                '<feGaussianBlur in="SourceGraphic" stdDeviation="2" />' +
              '</filter>' +
            '</defs>').documentElement);

          glow.attr('filter', 'url(#blur)');
        }

        return svg;
      })(),

      ICON = L.icon({
        iconUrl: 'data:image/svg+xml;base64,' + btoa(BASE_SVG.prop('outerHTML')),
        iconSize: [ ICON_SIZE, ICON_SIZE ]
      }),

      SELECTED_ICON = L.icon({
        iconUrl: 'data:image/svg+xml;base64,' + btoa(SELECTED_SVG.prop('outerHTML')),
        iconSize: [ ICON_SIZE, ICON_SIZE ]
      });

  var PointMarker = function(latlng) {

    var marker = L.marker(latlng, { icon: ICON } ),

        isSelected = false,

        addTo = function(layer) {
          marker.addTo(layer);
        },

        onClick = function() {
          isSelected = !isSelected;
          if (isSelected)
            marker.setIcon(SELECTED_ICON);
          else
            marker.setIcon(ICON);
        };

    marker.on('click', onClick);

    this.addTo = addTo;
  };

  return PointMarker;

});
