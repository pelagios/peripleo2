define([], function() {

  var IIIFView = function(containerEl, depiction) {

    var iiifPane = L.map(containerEl[0], {
          center: [0, 0],
          crs: L.CRS.Simple,
          zoom: 0,
          maxZoom: 10,
          zoomControl: false
        }),

        iiifLayer =
          L.tileLayer.iiif(depiction.iiif_uri, { attribution: false, fitBounds: true }).addTo(iiifPane),

        destroy = function() {
          containerEl.empty();
        };

    this.destroy = destroy;
  };

  return IIIFView;

});
