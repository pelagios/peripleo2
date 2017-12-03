define([
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/facets/facetsPane',
  'ui/controls/search/filterpane/footer',
  'ui/controls/search/filterpane/timeHistogram'
], function(HasEvents, FacetsPane, Footer, TimeHistogram) {

  var SLIDE_OPTS = { duration: 180 };

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

        /** Clears the footer result count **/
        clearFooter = function() {
          footer.clear();
        },

        /** Toggles the visibility of the filter pane **/
        togglePane = function() {
          var isVisible = body.is(':visible'),
              action = (isVisible) ? 'slideUp' : 'slideDown';

          // Slide the pane element
          body.velocity(action, SLIDE_OPTS);

          // Set the footer button state
          footer.setOpen(!isVisible);

          // If the visibility changed, fire an event up the hierarchy
          if (isVisible) self.fireEvent('close');
          else self.fireEvent('open');
        },

        /** Sets the open/closed state of the pane - used externally **/
        setOpen = function(open) {
          var isVisible = body.is(':visible');

          // Slide the pane element if needed
          if (isVisible && !open)
            body.velocity('slideUp', SLIDE_OPTS);
          else if (!isVisible && open)
            body.velocity('slideDown', SLIDE_OPTS);

          // Set footer button state
          footer.setOpen(open);
        },

        setSearchResponse = function(response) {
          // Update facets and time histogram
          if (response.aggregations) {
            var byTime = response.aggregations.find(function(agg) {
                  return agg.name === 'by_time';
                }),

                timerange = response.request_args.timerange;

            facetsPane.update(response.aggregations);
            if (byTime) timeHistogram.update(byTime.buckets, timerange);
          }

          // Update footer
          footer.setSearchResponse(response);
        };

    facetsPane.on('setFilter', this.forwardEvent('setFilter'));
    timeHistogram.on('selectionChange', this.forwardEvent('timerangeChange'));
    footer.on('toggle', togglePane);

    this.clearFooter = clearFooter;
    this.setOpen = setOpen;
    this.setSearchResponse = setSearchResponse;
    this.setFilterByViewport = footer.setFilterByViewport;

    HasEvents.apply(this);
  };
  FilterPane.prototype = Object.create(HasEvents.prototype);

  return FilterPane;

});
