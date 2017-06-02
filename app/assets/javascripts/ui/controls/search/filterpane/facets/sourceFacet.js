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

        empty = function() {
          topSource.empty();
          moreBuckets.empty();
        },

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
                  // Redundant means a has same root and higher count
                  var pa = a.path[0],
                      pb = b.path[0];

                  if (pa.id === pb.id && a.count > b.count) {
                    redundant = a;
                    return false;
                  }
                });

                return redundant;
              };

          buckets.forEach(function(bucket) {
            var redundant = findRedundant(bucket);
            if (redundant) {
              // Keep the path with a higher count
              if (redundant.count < bucket.count)
                flattened[flattened.indexOf(redundant)] = bucket;
            } else {
              flattened.push(bucket);
            }
          });

          return flattened;
        },

        update = function(buckets) {
          empty();
          if (buckets.length > 0) {
            var parsed = buckets.map(function(obj) {
                  var key = Object.keys(obj)[0],

                      path = Object.keys(obj)[0].split(SEPARATOR + SEPARATOR).map(function(seg) {
                        var t = seg.split(SEPARATOR);
                        return { id: t[0], label: t[1] };
                      }),

                      count = obj[key];

                  return { path: path, count: count };
                }),

                flattened = flattenBuckets(parsed),

                path = flattened[0].path,
                more = flattened.length - 1,

                label =
                  (more > 0) ?
                    (more > 1) ?
                      (more > 10) ? 'and 10+ other sources' : 'and ' + more + ' other sources' :
                    'and 1 other source' :
                  '';

            path.forEach(function(seg) {
              topSource.append(
                '<span><a href="#" data-id="' + seg.id + '">' + seg.label + '</a></span>');
            });

            moreBuckets.html(label);
          }
        };

    this.update = update;
  };

  return SourceFacet;

});
