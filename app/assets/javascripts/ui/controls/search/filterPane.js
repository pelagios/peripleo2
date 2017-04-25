define([
  'ui/common/hasEvents',
  'ui/controls/search/footer',
  'ui/controls/search/timeHistogram'
], function(HasEvents, Footer, TimeHistogram) {

  var SLIDE_DURATION = 180;

  var FilterPane = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="filterpane">' +
            '<div id="filterpane-body"></div>' +
          '</div>').appendTo(parentEl),

        body = element.find('#filterpane-body').hide(),

        timeHistogramSection = jQuery('<div class="section"></div').appendTo(body),
        timeHistogram = new TimeHistogram(timeHistogramSection, 320, 40),

        footer = new Footer(element),

        getAggregation = function(response, name) {
          var aggregation = response.aggregations.find(function(agg) {
            return agg.name === name;
          });

          if (aggregation) return aggregation.buckets;
        },

        togglePane = function() {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';

          body.velocity(action, { duration: SLIDE_DURATION });

          if (visible) self.fireEvent('close');
          else self.fireEvent('open');
        },

        setResponse = function(response) {
          footer.update(response);
          if (response.aggregations) {
            timeHistogram.update(getAggregation(response, 'by_time'));
          }
        },

        setOpen = function(open) {
          var visible = body.is(':visible');
          if (visible != open) togglePane();
        };

    timeHistogram.on('selectionChange', this.forwardEvent('timerangeChange'));
    footer.on('toggle', togglePane);

    this.setOpen = setOpen;
    this.setResponse = setResponse;

    HasEvents.apply(this);
  };
  FilterPane.prototype = Object.create(HasEvents.prototype);

  return FilterPane;

});
