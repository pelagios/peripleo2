define(['ui/common/formatting'], function(Formatting) {

  var IIIFView = function(containerEl, depiction) {

    var attributionEl = containerEl.find('.attribution'),

        iiifPane = L.map(containerEl[0], {
          center: [0, 0],
          crs: L.CRS.Simple,
          zoom: 0,
          zoomControl: false
        }),

        iiifLayer =
          L.tileLayer.iiif(depiction.iiif_uri, { attribution: false, fitBounds: true }).addTo(iiifPane),

        destroy = function() {
          try {
            iiifPane.remove();
          } catch (e) {
            // This can fail if the user destroys too early (i.e. before the IIIF has loaded)
          }
          containerEl.empty();
        };

    attributionEl.html(Formatting.formatClickableURL(depiction.source));
    containerEl.prepend('<img class="iiif-logo" src="/assets/images/iiif-logo.png">');

    this.destroy = destroy;
  };

  return IIIFView;

});
