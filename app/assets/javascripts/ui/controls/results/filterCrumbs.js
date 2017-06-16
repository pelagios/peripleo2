define([], function() {

  var SLIDE_DURATION = 100;

  var ICONS = {
    'PLACE' : '&#xf041;'
  };

  var Header = function(parentEl) {

    var el = jQuery(
          '<div class="rl-h-container">' +
            '<ul class="rl-h-filters"></ul>' +
            '<div class="rl-h-clear icon stroke7">&#xe680;</div>' +
          '</div>').appendTo(parentEl),

        filterList = el.find('.rl-h-filters'),

        isVisible = function() {
          return parentEl.is(':visible');
        },

        show = function() {
          if (!parentEl.is(':visible'))
            parentEl.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        hide = function() {
          if (parentEl.is(':visible'))
            parentEl.velocity('slideUp', { duration: SLIDE_DURATION });
        },

        addFilter = function(type, label, identifier) {
          filterList.append(
            '<li>' +
              '<span class="icon">' + ICONS[type] + '</span>' +
              '<a class="label destination" href="#">' + label + '</a>' +
            '</li>');
        },

        pushPlace = function(reference) {
          addFilter('PLACE', reference.title, reference.identifiers[0]);
        },

        clear = function() {
          filterList.empty();
        };

    this.isVisible = isVisible;
    this.show = show;
    this.hide = hide;
    this.clear = clear;
    this.pushPlace = pushPlace;
  };

  return Header;

});
