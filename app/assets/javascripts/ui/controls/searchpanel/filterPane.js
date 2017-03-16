define([
  'ui/common/hasEvents',
  'ui/controls/searchpanel/footer',
  'ui/controls/searchpanel/timeHistogram'
], function(HasEvents, Footer, TimeHistogram) {

  var SLIDE_DURATION = 180;

  var FilterPane = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="filterpane">' +
            '<div id="filterpane-body"></div>' +
          '</div>').appendTo(parentEl),

        body = element.find('#filterpane-body'),

        timeHistogramSection = jQuery('<div class="section"></div').appendTo(body),
        timeHistogram = new TimeHistogram(timeHistogramSection, 320, 40),

        footer = new Footer(element),

        togglePane = function() {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';

          body.velocity(action, { duration: SLIDE_DURATION });

          if (visible) self.fireEvent('close');
          else self.fireEvent('open');
        },

        update = function(searchResponse) {
          timeHistogram.update(searchResponse);
        };

    body.hide();

    footer.on('toggle', togglePane);

    this.update = update;

    HasEvents.apply(this);
  };
  FilterPane.prototype = Object.create(HasEvents.prototype);

  return FilterPane;

});
