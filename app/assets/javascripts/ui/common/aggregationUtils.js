define([], function() {

  var SEPARATOR = '\u0007';

  return {

    /** Returns the aggregation with the specified name **/
    getAggregation : function(aggs, name) {
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

    /**
     * 'Flattens' buckets so that redundant parent buckets are removed.
     *
     * Example:
     * If 'Graz>Coin Collection' contains 10 hits, there will
     * be a redundant parent bucket 'Graz' with 10 hits. This bucket will be removed.
     */
    flattenBuckets : function(buckets) {
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

    getCountForId : function(buckets, id) {
      var matches = buckets.filter(function(b) {
            var leaf = b.path[b.path.length - 1];
            return leaf.id === id;
          });

      if (matches.length > 0)
        return matches[0].count;
      else
        return 0;
    }

  };

});
