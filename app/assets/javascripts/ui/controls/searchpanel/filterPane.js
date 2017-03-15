define([
  'ui/common/hasEvents',
  'ui/controls/searchpanel/footer'
], function(HasEvents, Footer) {

  var FilterPane = function(parentEl) {

    var element = jQuery(
          '<div id="filterpane">' +
            '<div id="filterpane-body"></div>' +
          '</div>').appendTo(parentEl),

        footer = new Footer(element);

    HasEvents.apply(this);
  };
  FilterPane.prototype = Object.create(HasEvents.prototype);

  return FilterPane;

});
