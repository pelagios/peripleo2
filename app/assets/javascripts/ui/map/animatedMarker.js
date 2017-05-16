define([], function() {

  var STYLE = {

        STROKE_COLOR   : '#a64a40',
        STROKE_OPACITY : 1,
        STROKE_WIDTH   : 1.5,

        FILL_COLOR     : '#e75444',
        FILL_OPACITY   : 1,

        GLOW_RADIUS    : 25,
        GLOW_FILL      : 'rgba(255, 0, 0, 0.4)',
        GLOW_BLURRED   : false

      },

      canvasSize = function(markerSize) {
        // TODO scale with size
        return 8  * STYLE.GLOW_RADIUS / 3;
      },

      baseSVG = function(size) {
            // TODO scale with size
        var w = canvasSize(size),

            svg = jQuery(
              '<svg xmlns="http://www.w3.org/2000/svg" version="1.1">' +
                '<path class="leaflet-interactive" stroke-linecap="round" stroke-linejoin="round" ' +
                  'fill-rule="evenodd"></path>' +
              '</svg>'),

            path = svg.find('path'),

            x = (w - size) / 2 - STYLE.STROKE_WIDTH,

            d = 'M' + x + ',' + (w / 2) + 'a' + size + ',' + size +' 0 1,0 '+ (2 * size) + ',0 a' +
              size + ',' + size + ' 0 1,0 -' + (2 * size) + ',0';

        svg.attr('width', w);
        svg.attr('height', w);

        path.attr('stroke', STYLE.STROKE_COLOR);
        path.attr('stroke-opacity', STYLE.STROKE_OPACITY);
        path.attr('stroke-width', STYLE.STROKE_WIDTH);
        path.attr('fill', STYLE.FILL_COLOR);
        path.attr('fill-opacity', STYLE.FILL_OPACITY);
        path.attr('d', d);

        return svg;
      },

      selectedSVG = function(baseSVG) {
        var svg = baseSVG.clone(),
            offset = svg.attr('width') / 2,
            glow = jQuery('<circle></circle>');

        glow.attr('cx', offset);
        glow.attr('cy', offset);
        glow.attr('r', STYLE.GLOW_RADIUS);
        glow.attr('fill', STYLE.GLOW_FILL);

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
      },

      growAnim = function(selectedSVG) {
        var svg = selectedSVG.clone(),
            glow = svg.find('circle');

        glow.append(jQuery.parseXML(
          '<animate attributeName="r" begin="0s" dur="0.1s" repeatCount="1" from="0" ' +
          'to="' + STYLE.GLOW_RADIUS  +'"></animate>').documentElement);

        return svg;
      },

      shrinkAnim = function(selectedSVG) {
        var svg = selectedSVG.clone(),
            glow = svg.find('circle');

        glow.attr('r', 0);

        glow.append(jQuery.parseXML('<animate attributeName="r" begin="0s" dur="0.1s" repeatCount="1" ' +
          'from="' + STYLE.GLOW_RADIUS + '" to="0"></animate>').documentElement);

        return svg;
      };

  var AnimatedMarker = function(latlng, size) {

    var self = this,

        isSelected = false,

        CANVAS_SIZE = canvasSize(size),

        BASE_SVG = baseSVG(size),

        SELECTED_SVG = selectedSVG(BASE_SVG),

        GROW_ANIM = growAnim(SELECTED_SVG),

        SHRINK_ANIM = shrinkAnim(SELECTED_SVG),

        ICON = L.icon({
          iconUrl: 'data:image/svg+xml;base64,' + btoa(BASE_SVG.prop('outerHTML')),
          iconSize: [ CANVAS_SIZE, CANVAS_SIZE ]
        }),

        ICON_SELECTED = L.icon({
          iconUrl: 'data:image/svg+xml;base64,' + btoa(SELECTED_SVG.prop('outerHTML')),
          iconSize: [ CANVAS_SIZE, CANVAS_SIZE ]
        }),

        animate = function(anim, iconAfter) {
          self.setIcon(L.icon({
            iconUrl: 'data:image/svg+xml;base64,' + btoa(anim.prop('outerHTML')),
            iconSize: [ CANVAS_SIZE, CANVAS_SIZE ]
          }));

          // After animation, remove
          setTimeout(function() {
            self.setIcon(iconAfter);
          }, 200);
        },

        onClick = function() {
          if (isSelected) deselect();
          else select();
        },

        select = function() {
          if (!isSelected) {
            isSelected = true;
            animate(GROW_ANIM, ICON_SELECTED);
          }
        },

        deselect = function() {
          if (isSelected) {
            isSelected = false;
            animate(SHRINK_ANIM, ICON);
          }
        };

    self.on('click', onClick);

    this.isSelected = function() { return isSelected; };
    this.select = select;
    this.deselect = deselect;

    L.Marker.apply(this, [ latlng, { icon: ICON } ]);
  };
  AnimatedMarker.prototype = Object.create(L.Marker.prototype);

  return AnimatedMarker;

});
