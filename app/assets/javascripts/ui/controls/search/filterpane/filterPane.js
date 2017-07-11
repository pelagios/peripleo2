define([
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/facetsPane',
  'ui/controls/search/filterpane/footer',
  'ui/controls/search/filterpane/timeHistogram'
], function(HasEvents, FacetsPane, Footer, TimeHistogram) {

  var SLIDE_DURATION = 180;

  var FilterPane = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="filterpane">' +
            '<div id="filterpane-body"></div>' +
          '</div>').appendTo(parentEl),

        body = element.find('#filterpane-body').hide(),

        facetsPane    = new FacetsPane(body),
        timeHistogram = new TimeHistogram(body, 320, 40),
        footer        = new Footer(element),

        togglePane = function(e) {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';

          body.velocity(action, { duration: SLIDE_DURATION });

          if (visible) self.fireEvent('close');
          else self.fireEvent('open');
        },

        setSearchResponse = function(response) {
          if (response.aggregations) {
            var byTime = response.aggregations.find(function(agg) {
                  return agg.name === 'by_time'; });

            if (byTime) timeHistogram.update(byTime.buckets);
            facetsPane.update(response.aggregations);
          }

          footer.update(response);
        },

        setOpen = function(open) {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';

          footer.setOpen(open);

          if ((visible && !open) || (!visible && open))
            body.velocity(action, { duration: SLIDE_DURATION });
        };

    facetsPane.on('setFilter', this.forwardEvent('setFilter'));
    timeHistogram.on('selectionChange', this.forwardEvent('timerangeChange'));
    footer.on('toggle', togglePane);

    this.setOpen = setOpen;
    this.setSearchResponse = setResponse;
    this.setFilterByViewport = footer.setFilterByViewport;

    HasEvents.apply(this);
  };
  FilterPane.prototype = Object.create(HasEvents.prototype);

  return FilterPane;

});
