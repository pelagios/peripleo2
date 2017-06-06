define([], function() {

  // Some (sub)facets are irrelevant, e.g. DATASET sub-types (AUTHORITY, AUTHORITY_GAZETTEER, etc.)
  var RELEVANT_FACETS = [ 'PLACE', 'OBJECT', 'PERSON', 'DATASET' ];

  var TypeFacet = function(parentEl) {
    var el = jQuery(
          '<div class="type-graph">' +
            '<div class="bar"></div>' +
            '<div class="clickbuffer"></div>' +
          '</div>').prependTo(parentEl),

       bar = el.find('.bar'),

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
       };

    this.update = update;

  };

  return TypeFacet;

});
