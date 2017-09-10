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
          containerEl.empty();
        };

    attributionEl.html(Formatting.formatClickableURL(depiction.source));
    containerEl.prepend('<img class="iiif-logo" src="/assets/images/iiif-logo.png">');

    this.destroy = destroy;
  };

  return IIIFView;

});
