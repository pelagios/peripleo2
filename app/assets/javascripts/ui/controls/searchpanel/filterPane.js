define([
  'ui/common/hasEvents',
  'ui/controls/searchpanel/footer'
], function(HasEvents, Footer) {

  var SLIDE_DURATION = 180;

  var FilterPane = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="filterpane">' +
            '<div id="filterpane-body"></div>' +
          '</div>').appendTo(parentEl),

        body = element.find('#filterpane-body'),

        footer = new Footer(element),

        togglePane = function() {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';

          body.velocity(action, { duration: SLIDE_DURATION });

          // TODO fire events
          
        };

    body.hide();
    footer.on('togglePane', togglePane);

    HasEvents.apply(this);
  };
  FilterPane.prototype = Object.create(HasEvents.prototype);

  return FilterPane;

});
