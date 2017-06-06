define([
  'ui/controls/search/filterpane/facets/typeGraph'
], function(TypeGraph) {

  var FacetsOverview = function(parentEl) {

    var SEPARATOR = '\u0007';

    var el = jQuery(
          '<div class="facets-overview">' +
            '<div class="facets-row">' +
              '<div class="facet sources">' +
                '<span class="icon">&#xf187;</span>' +
                '<span class="value"><span class="count">0</span> sources</span>' +
              '</div>' +

              '<div class="facet categories">' +
                '<span class="icon">&#xf02b;</span>' +
                '<span class="value"><span class="count">0</span> topics</span>' +
              '</div>' +

              '<div class="facet people">' +
                '<span class="icon">&#xf007;</span>' +
                '<span class="value"><span class="count">0</span> people</span>' +
              '</div>' +

              '<div class="facet periods">' +
                '<span class="icon">&#xf017;</span>' +
                '<span class="value"><span class="count">0</span> periods</span>' +
              '</div>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        typeGraph = new TypeGraph(el),
        typeFacet = el.find('.type-divider'),

        sourceCount = el.find('.facet.sources .count'),

        /**
         * 'Flattens' buckets so that redundant parent buckets are removed.
         *
         * Example:
         * If 'Graz>Coin Collection' contains 10 hits, there will
         * be a redundant parent bucket 'Graz' with 10 hits. This bucket will be removed.
         */
        flattenBuckets = function(buckets) {
          var flattened = [],

              findRedundant = function(b) {
                var redundant;

                jQuery.each(flattened, function(idx, a) {
                  // Redundant means a has same root and higher or equal count
                  var pa = a.path[0],
                      pb = b.path[0];

                  if (pa.id === pb.id && a.count >= b.count) {
                    redundant = a;
                    return false;
                  }
                });

                return redundant;
              };

          buckets.forEach(function(bucket) {
            var redundant = findRedundant(bucket);
            if (redundant) {
              // Keep the path with a higher count, or longer path if count is equal
              if (redundant.count < bucket.count || redundant.path.length < bucket.path.length)
                flattened[flattened.indexOf(redundant)] = bucket;
            } else {
              flattened.push(bucket);
            }
          });

          return flattened;
        },

        getAggregation = function(aggs, name) {
          var aggregation = aggs.find(function(agg) {
                return agg.name === name;
              }),

              parseBuckets = function(asObj) {
                return asObj.map(function(obj) {
                  var key = Object.keys(obj)[0],

                      path = Object.keys(obj)[0].split(SEPARATOR + SEPARATOR).map(function(seg) {
                        var t = seg.split(SEPARATOR);
                        return { id: t[0], label: t[1] };
                      }),

                      count = obj[key];
                  return { path: path, count: count };
                });
              };

          if (aggregation) return parseBuckets(aggregation.buckets);
        },

        update = function(aggs) {
          var byType = getAggregation(aggs, 'by_type'),
              bySource = getAggregation(aggs, 'by_dataset'),
              byCategory = getAggregation(aggs, 'by_category'),
              topPeople = getAggregation(aggs, 'top_people'),
              topPeriods = getAggregation(aggs, 'top_periods');

          if (byType) typeGraph.update(byType);

          if (bySource) sourceCount.html(flattenBuckets(bySource).length);
          else sourceCount.html('0');
        };

    this.update = update;
  };

  return FacetsOverview;

});
