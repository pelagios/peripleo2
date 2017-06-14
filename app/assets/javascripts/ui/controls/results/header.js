define([], function() {

  var SLIDE_DURATION = 100;

  var Header = function(parentEl) {

    var el = jQuery(
          '<div class="rl-h-container">' +
            // '<div class="rl-h-label icon">&#xf0b0;</div>' +
            '<ul class="rl-h-filters"></ul>' +
            '<div class="rl-h-clear"><span class="icon stroke7">&#xe680;</span></div>' +
          '</div>').appendTo(parentEl),

        filterList = el.find('.rl-h-filters'),

        hide = function() {
          if (parentEl.is(':visible'))
            parentEl.velocity('slideUp', { duration: SLIDE_DURATION });
        };

    /* Dummy - just to test the design

      filterList.append(
        '<li>' +
          '<span class="icon">&#xf187;</span>' +
          '<a class="label destination" href="#">University of Graz</a>' +
        '</li>');

        filterList.append(
          '<li>' +
            '<span class="icon">&#xf041;</span>' +
            '<a class="label destination" href="#">Roma</a>' +
          '</li>');


      filterList.append(
        '<li class="collapsed">' +
          '<span class="icon">&#xf017;</span>' +
        '</li>');*/

    this.hide = hide;
  };

  return Header;

});
