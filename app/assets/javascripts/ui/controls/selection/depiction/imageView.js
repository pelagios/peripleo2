define([
  'ui/api',
  'ui/common/formatting'
], function(API, Formatting) {

  var ImageView = function(containerEl, depiction) {

    var attributionEl = containerEl.find('.attribution'),

        preloadImage = function(url) {
          var d = jQuery.Deferred(),
              img = new Image();

          img.onerror = function() {
            d.reject(url);
          };

          img.onload = function() {
            d.resolve(img);
          };

          img.src = url;
          return d.promise();
        },

        destroy = function() {
          containerEl.css('background-image', 'none');
        };

    preloadImage(depiction.url).done(function(img) {
      containerEl.css('background-image', 'url("' + img.src + '")');
    }).fail(function(url) {
      API.reportBrokenLink(depiction.source, url);
    });

    attributionEl.html(Formatting.formatClickableURL(depiction.source));

    this.destroy = destroy;
  };

  return ImageView;

});
