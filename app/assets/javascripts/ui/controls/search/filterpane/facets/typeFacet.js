define([
  'ui/controls/search/filterpane/facets/typeCounts'
], function(TypeCounts) {

  // Some (sub)facets are irrelevant, e.g. DATASET sub-types (AUTHORITY, AUTHORITY_GAZETTEER, etc.)
  var RELEVANT_FACETS = [ 'PLACE', 'OBJECT', 'PERSON', 'DATASET' ];

  var TypeFacet = function(graphEl, countsEl) {
    var bar = jQuery('<div class="bar"></div>').appendTo(graphEl),
        clickbuffer = jQuery('<div class="clickbuffer"></div>').appendTo(graphEl),

        typeCounts = new TypeCounts(countsEl),

        getTotalCount = function(buckets) {
          var total = 0;
          buckets.forEach(function(b) {
            var type = b.path[0].id;
            if (RELEVANT_FACETS.indexOf(type) > -1)
              total += b.count;
          });
          return total;
        },

        update = function(buckets) {
          var isRelevantBucket = function(b) {
                var type = b.path[0].id;
                return RELEVANT_FACETS.indexOf(type) > -1;
              },

              relevantBuckets = buckets.filter(isRelevantBucket),

              totalCount = relevantBuckets.reduce(function(acc, b) {
                return acc + b.count;
              }, 0);

              addSegment = function(b) {
                var minW = 1.5, // Make sure even small buckets remain visible
                    maxW = 100 - (relevantBuckets.length - 1) * minW,
                    w = 100 * b.count / totalCount,
                    normalized = Math.min(Math.max(w, minW), maxW);

                bar.append('<span class="segment ' + b.path[0].id + '" style="width:' + normalized + '%"></span>');
              };

          bar.empty();
          relevantBuckets.forEach(addSegment);
          
          // TODO only update when visible
          typeCounts.update(relevantBuckets);
        };

    this.update = update;

  };

  return TypeFacet;

});
