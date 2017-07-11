define([
  'ui/common/aggregationUtils',
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/facets/facetChart',
  'ui/controls/search/filterpane/facets/typeFacet'
], function(AggregationUtils, HasEvents, FacetChart, TypeFacet) {

  var FacetSection = function(parentEl) {

    var el = jQuery(
          '<div class="facets-pane">' +
            '<div class="summary-section">' +
              '<div class="summary-section-inner">' +
                '<div class="sliding-pane">' +
                  // Section 1: facet counts
                  '<div class="summary-row facets">' +
                    '<ul>' +
                      '<li class="col sources" data-facet="sources">' +
                        '<span class="icon">&#xf187;</span>' +
                        '<span class="value"><span class="count">0</span> sources</span>' +
                      '</li>' +

                      '<li class="col topics" data-facet="topics">' +
                        '<span class="icon">&#xf02b;</span>' +
                        '<span class="value"><span class="count">0</span> topics</span>' +
                      '</li>' +

                      '<li class="col people" data-facet="people">' +
                        '<span class="icon">&#xf007;</span>' +
                        '<span class="value"><span class="count">0</span> people</span>' +
                      '</li>' +

                      '<li class="col periods" data-facet="periods">' +
                        '<span class="icon">&#xf017;</span>' +
                        '<span class="value"><span class="count">0</span> periods</span>' +
                      '</li>' +
                    '</ul>' +
                  '</div>' +
                  // Section 2: type counts
                  '<div class="summary-row types"></div>' +
                '</div>' + // .sliding-pane
              '</div>' + //.summary-section-inner
            '</div>' + // .summary-section
            '<div class="type-bar"></div>' + // Multicolor item type 'piechart' bar
            '<div class="chart-section"></div>' + // Collapsible section for detail facet charts
          '</div>').appendTo(parentEl),

        typeGraph = el.find('.type-bar'),
        slidingPane = el.find('.sliding-pane'),

        typeFacet = new TypeFacet(el.find('.type-bar'), el.find('.summary-row.types')),

        sourceCount = el.find('.col.sources .count'),
        topicCount  = el.find('.col.topics .count'),
        peopleCount = el.find('.col.people .count'),
        periodCount = el.find('.col.periods .count'),

        // TODO dummy only
        facetChart = new FacetChart(el.find('.chart-section').hide()),

        update = function(aggs) {
          var byType = AggregationUtils.getAggregation(aggs, 'by_type'),
              bySource = AggregationUtils.getAggregation(aggs, 'by_dataset'),
              byCategory = AggregationUtils.getAggregation(aggs, 'by_category'),
              topPeople = AggregationUtils.getAggregation(aggs, 'top_people'),
              topPeriods = AggregationUtils.getAggregation(aggs, 'top_periods');

          if (byType) typeFacet.update(byType);

          if (bySource) {
            // TODO dummy only - for testing
            sourceCount.html(AggregationUtils.flattenBuckets(bySource).length);
            facetChart.update(AggregationUtils.flattenBuckets(bySource));
          } else {
            sourceCount.html('0');
          }
        },

        toggleSlidePane = function() {
          var offset = parseInt(slidingPane.css('top')),
              top = (offset === 0) ? -38 : 0;

          slidingPane.velocity({ top: top }, { duration: 200 });
        },

        onShowDetails = function(e) {
          var col = jQuery(e.target).closest('.col'),
              facet = col.data('facet');

          // TODO dummy
          facetChart.toggle();
        };

    typeGraph.click(toggleSlidePane);

    typeFacet.on('setFilter', this.forwardEvent('setFilter'));
    facetChart.on('setFilter', this.forwardEvent('setFilter'));

    el.on('click', '.col', onShowDetails);

    this.update = update;

    HasEvents.apply(this);
  };
  FacetSection.prototype = Object.create(HasEvents.prototype);

  return FacetSection;

});
