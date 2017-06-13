define([], function() {

  var SLIDE_DURATION = 100;

  var Header = function(parentEl) {

    var el = jQuery(
          '<div class="rl-h-container">' +
            '<div class="rl-h-label icon">&#xf0b0;</div>' +
            '<div class="rl-h-filters"></div>' +
            '<div class="rl-h-clear"><span class="icon stroke7">&#xe680;</span></div>' +
          '</div>').appendTo(parentEl);

        hide = function() {
          if (parentEl.is(':visible'))
            parentEl.velocity('slideUp', { duration: SLIDE_DURATION });
        };

    // TODO
    this.hide = function() {};

  };

  return Header;

});
