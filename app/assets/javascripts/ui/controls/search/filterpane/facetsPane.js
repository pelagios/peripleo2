define([
  'ui/common/aggregationUtils',
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/facets/typeFacet'
], function(AggregationUtils, HasEvents, TypeFacet) {

  var FacetsOverview = function(parentEl) {

    var el = jQuery(
          '<div class="facets-pane">' +
            '<div class="info-section">' +
              '<div class="info-section-inner">' +
                '<div class="sliding-pane">' +
                  // Section 1: facet counts
                  '<div class="info-row facets">' +
                    '<ul>' +
                      '<li class="col sources">' +
                        '<span class="icon">&#xf187;</span>' +
                        '<span class="value"><span class="count">0</span> sources</span>' +
                      '</li>' +

                      '<li class="col topics">' +
                        '<span class="icon">&#xf02b;</span>' +
                        '<span class="value"><span class="count">0</span> topics</span>' +
                      '</li>' +

                      '<li class="col people">' +
                        '<span class="icon">&#xf007;</span>' +
                        '<span class="value"><span class="count">0</span> people</span>' +
                      '</li>' +

                      '<li class="col periods">' +
                        '<span class="icon">&#xf017;</span>' +
                        '<span class="value"><span class="count">0</span> periods</span>' +
                      '</li>' +
                    '</ul>' +
                  '</div>' +
                  // Section 2: type counts
                  '<div class="info-row types"></div>' +
                '</div>' + // .sliding-pane
              '</div>' + //.info-section-inner
            '</div>' + // .info-section

            '<div class="type-graph"></div>' +
          '</div>').appendTo(parentEl),

        typeGraph = el.find('.type-graph'),
        slidingPane = el.find('.sliding-pane'),

        typeFacet = new TypeFacet(el.find('.type-graph'), el.find('.info-row.types')),

        sourceCount = el.find('.col.sources .count'),
        topicCount  = el.find('.col.topics .count'),
        peopleCount = el.find('.col.people .count'),
        periodCount = el.find('.col.periods .count'),

        update = function(aggs) {
          var byType = AggregationUtils.getAggregation(aggs, 'by_type'),
              bySource = AggregationUtils.getAggregation(aggs, 'by_dataset'),
              byCategory = AggregationUtils.getAggregation(aggs, 'by_category'),
              topPeople = AggregationUtils.getAggregation(aggs, 'top_people'),
              topPeriods = AggregationUtils.getAggregation(aggs, 'top_periods');

          if (byType) typeFacet.update(byType);

          if (bySource) sourceCount.html(AggregationUtils.flattenBuckets(bySource).length);
          else sourceCount.html('0');
        },

        toggleSlidePane = function() {
          var offset = parseInt(slidingPane.css('top')),
              top = (offset === 0) ? -38 : 0;

          slidingPane.velocity({ top: top }, { duration: 200 });
        };

    typeGraph.click(toggleSlidePane);
    typeFacet.on('setFilter', this.forwardEvent('setFilter'));

    this.update = update;

    HasEvents.apply(this);
  };
  FacetsOverview.prototype = Object.create(HasEvents.prototype);

  return FacetsOverview;

});
