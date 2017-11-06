define(['ui/common/formatting'], function(Formatting) {

  var IS_TOUCH = 'ontouchstart' in window || navigator.MaxTouchPoints > 0;

  var IIIFView = function(containerEl, depiction) {

    var parentEl = containerEl.parent(),

        attributionEl = containerEl.find('.attribution'),

        btnFullscreen = jQuery(
          '<div class="iiif-fullscreen icon">&#xf065;</div>').appendTo(containerEl),

        iiifPane = L.map(containerEl[0], {
          center: [0, 0],
          crs: L.CRS.Simple,
          zoom: 0,
          zoomControl: false
        }),

        iiifLayer =
          L.tileLayer.iiif(depiction.url, { attribution: false, fitBounds: true }).addTo(iiifPane),

        /**
         * Tablets don't have 'F11' fullscreen. We need to fall back to a standard
         * fullscreen-sized DIV. In order to achieve this, however, we need to detach the
         * the DIV from the original parent and attach it directly to the body element.
         */
        toggleFullscreen = function() {
          if (IS_TOUCH) {
            if (iiifPane.isFullscreen())
              parentEl.prepend(containerEl);
            else
              jQuery(document.body).append(containerEl);
          } else {
            iiifPane.toggleFullscreen();
          }

          iiifPane.toggleFullscreen({ pseudoFullscreen: true });
        },

        destroy = function() {
          try {
            iiifPane.remove();
          } catch (e) {
            // This can fail if the user destroys too early (i.e. before the IIIF has loaded)
          }
          containerEl.empty();
        };

    btnFullscreen.click(toggleFullscreen);

    attributionEl.html(Formatting.formatClickableURL(depiction.source));
    containerEl.prepend('<img class="iiif-logo" src="/assets/images/iiif-logo.png">');

    this.destroy = destroy;
  };

  return IIIFView;

});
