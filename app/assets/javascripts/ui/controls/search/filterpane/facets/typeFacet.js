define(['ui/common/formatting'], function(Formatting) {

  // Some (sub)facets are irrelevant, e.g. DATASET sub-types (AUTHORITY, AUTHORITY_GAZETTEER, etc.)
  var RELEVANT_FACETS = [ 'PLACE', 'OBJECT', 'PERSON', 'DATASET' ];

  var TypeFacet = function(graphEl, countsEl) {
    var bar = jQuery(
          '<div class="bar"></div>').appendTo(graphEl),

        clickbuffer = jQuery(
          '<div class="clickbuffer"></div>').appendTo(graphEl),

        counts = jQuery(
          '<ul></ul>').appendTo(countsEl),

        getTotalCount = function(buckets) {
          var total = 0;
          buckets.forEach(function(b) {
            var type = b.path[0].id;
            if (RELEVANT_FACETS.indexOf(type) > -1)
              total += b.count;
          });
          return total;
        },

        updateCounts = function(buckets) {
          counts.empty();
          buckets.forEach(function(b) {
            var t = b.path[0].id;
            counts.append(
              '<li class="col ' + t + '">' +
                '<span class="value">' +
                  '<span class="count">' + Formatting.formatNumber(b.count) + '</span> results' + 
                '</span>' +
              '</li>');
          });
        },

        updateBar = function(buckets) {
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
        },

        update = function(buckets) {
          updateBar(buckets);
          // TODO only when visible
          updateCounts(buckets);
        };

    this.update = update;

  };

  return TypeFacet;

});
