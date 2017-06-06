define([
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/facetsOverview',
  'ui/controls/search/filterpane/footer',
  'ui/controls/search/filterpane/timeHistogram'
], function(HasEvents, FacetsOverview, Footer, TimeHistogram) {

  var SLIDE_DURATION = 180;

  var FilterPane = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="filterpane">' +
            '<div id="filterpane-body">' +
              '<div class="facets"></div>' +
              '<!-- div class="hint-more">' +
                '<span class="icon">&#xf080;</span>' +
                '<a class="label" href="#">All stats and filters</a>' +
              '</div -->' +
            '</div>' +
          '</div>').appendTo(parentEl),

        body = element.find('#filterpane-body').hide(),
        facetsEl = body.find('.facets'),

        facetsOverview = new FacetsOverview(facetsEl),
        timeHistogram = new TimeHistogram(facetsEl, 320, 40),
        footer = new Footer(element),

        togglePane = function(e) {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';

          body.velocity(action, { duration: SLIDE_DURATION });

          if (visible) self.fireEvent('close');
          else self.fireEvent('open');
        },

        setResponse = function(response) {
          if (response.aggregations) {
            var byTime = response.aggregations.find(function(agg) {
                  return agg.name === 'by_time';
                });

            if (byTime) timeHistogram.update(byTime);
            facetsOverview.update(response.aggregations);
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

    timeHistogram.on('selectionChange', this.forwardEvent('timerangeChange'));
    footer.on('toggle', togglePane);

    this.setOpen = setOpen;
    this.setResponse = setResponse;

    HasEvents.apply(this);
  };
  FilterPane.prototype = Object.create(HasEvents.prototype);

  return FilterPane;

});
