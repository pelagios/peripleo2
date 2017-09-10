define(['ui/common/formatting'], function(Formatting) {

  var ImageView = function(containerEl, depiction) {

    var attributionEl = containerEl.find('.attribution'),

        destroy = function() {
          containerEl.css('background-image', 'none');
        };

    // TODO pre-load image & report in case of 404
    containerEl.css('background-image', 'url(' + depiction.url + ')');
    attributionEl.html(Formatting.formatClickableURL(depiction.source));

    this.destroy = destroy;
  };

  return ImageView;

});
