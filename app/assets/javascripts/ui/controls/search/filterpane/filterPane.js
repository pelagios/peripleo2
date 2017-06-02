define([
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/facets/peopleFacet',
  'ui/controls/search/filterpane/facets/periodFacet',
  'ui/controls/search/filterpane/facets/sourceFacet',
  'ui/controls/search/filterpane/facets/typeFacet',
  'ui/controls/search/filterpane/footer',
  'ui/controls/search/filterpane/timeHistogram'
], function(HasEvents, PeopleFacet, PeriodFacet, SourceFacet, TypeFacet, Footer, TimeHistogram) {

  var SLIDE_DURATION = 180;

  var FilterPane = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="filterpane">' +
            '<div id="filterpane-body">' +
              '<div class="facets"></div>' +
              '<div class="hint-more">' +
                '<span class="icon">&#xf080;</span>' +
                '<a class="label" href="#">All stats and filters</a>' +
              '</div>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        body = element.find('#filterpane-body').hide(),
        facetsEl = body.find('.facets'),

        timeHistogramSection = jQuery('<div class="timehistogram-section"></div>').appendTo(facetsEl),
        timeHistogram = new TimeHistogram(timeHistogramSection, 320, 40),

        facetSection = jQuery('<div class="termfacet-section"></div>').appendTo(facetsEl),
        typeFacet = new TypeFacet(facetSection),
        sourceFacet = new SourceFacet(facetSection),
        peopleFacet = new PeopleFacet(facetSection),
        periodFacet = new PeriodFacet(facetSection),

        footer = new Footer(element),

        getAggregation = function(response, name) {
          var aggregation = response.aggregations.find(function(agg) {
            return agg.name === name;
          });

          if (aggregation) return aggregation.buckets;
        },

        togglePane = function(e) {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';

          body.velocity(action, { duration: SLIDE_DURATION });

          if (visible) self.fireEvent('close');
          else self.fireEvent('open');
        },

        setResponse = function(response) {
          if (response.aggregations) {
            var byTime = getAggregation(response, 'by_time'),
                byType = getAggregation(response, 'by_type'),
                bySource = getAggregation(response, 'by_dataset');

            if (byTime) timeHistogram.update(byTime);
            if (byType) typeFacet.update(byType);
            if (bySource) sourceFacet.update(bySource);
          }

          footer.update(response);
        },

        setOpen = function(open) {
          var visible = body.is(':visible'),
              action = (visible) ? 'slideUp' : 'slideDown';

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
