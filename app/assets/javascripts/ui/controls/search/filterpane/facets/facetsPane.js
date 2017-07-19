define([
  'ui/common/aggregationUtils',
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/facets/facetChart',
  'ui/controls/search/filterpane/facets/typeIndicator'
], function(AggregationUtils, HasEvents, FacetChart, TypeIndicator) {

  var FacetsPane = function(parentEl) {

    var element = jQuery(
          '<div class="facets-pane">' +
            '<div class="summary-section">' +
              '<div class="summary-section-inner">' +
                '<div class="sliding-pane">' +

                  // Top of the sliding panel: facet counts
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

                  // Bottom of the sliding panel: type counts
                  '<div class="summary-row types"></div>' +

                '</div>' +
              '</div>' +
            '</div>' +

            // The item type indicator bar
            '<div class="type-indicator"></div>' +

            // Facet details section (donut chart + top 3 list)
            '<div class="facet-details"></div>' +

          '</div>').appendTo(parentEl),

        slidingPane   = element.find('.sliding-pane'),

        typeIndicator = new TypeIndicator(element.find('.type-indicator'), element.find('.summary-row.types')),

        sourceCount = element.find('.col.sources .count'),
        topicCount  = element.find('.col.topics .count'),
        peopleCount = element.find('.col.people .count'),
        periodCount = element.find('.col.periods .count'),

        // TODO dummy only
        facetChart = new FacetChart(element.find('.facet-details').hide()),

        update = function(aggs) {
          var byType = AggregationUtils.getAggregation(aggs, 'by_type'),
              bySource = AggregationUtils.getAggregation(aggs, 'by_dataset'),
              byCategory = AggregationUtils.getAggregation(aggs, 'by_category'),
              topPeople = AggregationUtils.getAggregation(aggs, 'top_people'),
              topPeriods = AggregationUtils.getAggregation(aggs, 'top_periods');

          if (byType) typeIndicator.update(byType);

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

    typeIndicator.on('click', toggleSlidePane);
    typeIndicator.on('setFilter', this.forwardEvent('setFilter'));
    
    facetChart.on('setFilter', this.forwardEvent('setFilter'));

    element.on('click', '.col', onShowDetails);

    this.update = update;

    HasEvents.apply(this);
  };
  FacetsPane.prototype = Object.create(HasEvents.prototype);

  return FacetsPane;

});
