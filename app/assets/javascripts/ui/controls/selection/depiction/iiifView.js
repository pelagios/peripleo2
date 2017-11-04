define(['ui/common/formatting'], function(Formatting) {

  var IIIFView = function(containerEl, depiction) {
    
    var attributionEl = containerEl.find('.attribution'),

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

        toggleFullscreen = function() {
          iiifPane.toggleFullscreen();
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
