require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/controls/selection/depiction/iiifView'
], function(IIIFView) {

  jQuery(document).ready(function() {
    var hasIIIF = jQuery('.iiif').length > 0,

        initIIIF = function() {
          var container = jQuery('.iiif'),
              url = container.data('url'),
              source = container.data('source'),
              iiifView = new IIIFView(container, { url: url, source: source });
        };

    if (hasIIIF) initIIIF();

  });

});
