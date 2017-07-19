define([
  'ui/common/aggregationUtils',
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/facets/facetDetails',
  'ui/controls/search/filterpane/facets/typeIndicator'
], function(AggregationUtils, HasEvents, FacetDetails, TypeIndicator) {

  var SLIDE_DURATION = 200;

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

        slidingPane = element.find('.sliding-pane'),

        sourceCount = element.find('.col.sources .count'),
        topicCount  = element.find('.col.topics .count'),
        peopleCount = element.find('.col.people .count'),
        periodCount = element.find('.col.periods .count'),

        typeIndicator =
          new TypeIndicator(element.find('.type-indicator'), element.find('.summary-row.types')),

        facetDetails =
          new FacetDetails(element.find('.facet-details').hide()),

        /** Facet data from last update, as a hash { type -> data } **/
        facetData = {},

        /** Toggles between facet count overview and counts by item type **/
        toggleSlidePane = function() {
          var offset = parseInt(slidingPane.css('top')),
              top = (offset === 0) ? -38 : 0;
          slidingPane.velocity({ top: top }, { duration: SLIDE_DURATION });
        },

        update = function(aggs) {
              // Shorthand to get flattened buckets by key
          var getFacetData = function(key) {
                var agg = AggregationUtils.getAggregation(aggs, key);
                if (agg)
                  return AggregationUtils.flattenBuckets(agg);
              },

              renderCount = function(element, key) {
                var buckets = facetData[key];
                if (buckets)
                  element.html(buckets.length);
                else
                  element.html('0');
              },

              byType = AggregationUtils.getAggregation(aggs, 'by_type');

          // Store facet details data
          facetData.sources = getFacetData('by_dataset');
          facetData.topics  = getFacetData('by_category');
          facetData.people  = getFacetData('top_people');
          facetData.periods = getFacetData('top_periods');

          // Update type indicator and counts immediately
          if (byType) typeIndicator.update(byType);
          renderCount(sourceCount, 'sources');
          renderCount(topicCount,  'topics');
          renderCount(peopleCount, 'people');
          renderCount(periodCount, 'periods');
        },

        onShowDetails = function(e) {
          var col = jQuery(e.target).closest('.col'),
              facet = col.data('facet'),
              buckets = facetData[facet];

          if (buckets && buckets.length > 0) {
            facetDetails.update(facet, buckets);
            facetDetails.toggle();
          }
        };

    element.on('click', '.col', onShowDetails);

    typeIndicator.on('click', toggleSlidePane);
    typeIndicator.on('setFilter', this.forwardEvent('setFilter'));
    facetDetails.on('setFilter', this.forwardEvent('setFilter'));

    this.update = update;

    HasEvents.apply(this);
  };
  FacetsPane.prototype = Object.create(HasEvents.prototype);

  return FacetsPane;

});
