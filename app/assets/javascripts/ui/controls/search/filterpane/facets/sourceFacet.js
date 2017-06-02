define([
  'ui/common/formatting'
], function(Formatting) {

  var SEPARATOR = '\u0007';

  var SourceFacet = function(parentEl) {

    var el = jQuery(
          '<div class="facet by-source right">' +
            '<div class="facet-aligner">' +
              '<div>' +
                '<h3>Data from</h3>' +
                '<div class="top-source item-is-in"></div>' +
                '<div class="more-buckets"></div>' +
              '</div>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        topSource = el.find('.top-source'),
        moreBuckets = el.find('.more-buckets'),

        // Gets the top bucket, at the lowest leaf-level
        getTopBucket = function(buckets) {
              // Max count
          var topCount = buckets[0].val,

              // All buckets where count = maxCount
              topBuckets = buckets.filter(function(bucket) {
                return bucket.val === topCount;
              }),

              // Sort by longest path, i.e. no. separator chars
              topLeaf = topBuckets.sort(function(a, b) {
                return b.key.split(SEPARATOR).length - a.key.split(SEPARATOR).length;
              })[0];

          return topLeaf;
        },

        /** Removes all buckets in the same branch as the provided path, comparing the root **/
        removeBranch = function(buckets, path) {
          var root = path[0];
          return buckets.filter(function(seg) {
            return seg.key.indexOf(root.id) < 0;
          });
        },

        // TODO handle zero buckets case
        update = function(buckets) {

          console.log(buckets);
          
          var asArray = buckets.map(function(obj) {
                var key = Object.keys(obj)[0],
                    val = obj[key];
                return { key: key, val: val };
              }),

              top = getTopBucket(asArray),

              path = top.key.split(SEPARATOR + SEPARATOR).map(function(segments) {
                var t = segments.split(SEPARATOR);
                return { id: t[0], label: t[1] };
              }),

              more = removeBranch(asArray, path).length,

              label =
                (more > 0) ?
                  (more > 1) ?
                    (more > 10) ? 'and 10+ other sources' : 'and ' + more + ' other sources' :
                  'and 1 more source' :
                '';

          topSource.empty();
          path.forEach(function(seg) {
            topSource.append(
              '<span><a href="#" data-id="' + seg.id + '">' + seg.label + '</a></span>');
          });

          moreBuckets.html(label);
        };

    this.update = update;
  };

  return SourceFacet;

});
