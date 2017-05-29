define([
  'ui/common/formatting'
], function(Formatting) {

  var TypeFacet = function(parentEl) {

    var el = jQuery(
          '<div class="facet by-type left">' +
            '<ul>' +
              '<li>' +
                '<div class="count object">' +
                  '<span class="icon">&#xf219;</span><span class="label">1,201</span>' +
                '</div>' +
              '</li>' +

              '<li style="width:65%">' +
                '<div class="count place">' +
                  '<span class="icon">&#xf041;</span><span class="label">548</span>' +
                '</div>' +
              '</li>' +

              '<li style="width:35%">' +
                '<div class="count person">' +
                  '<span class="icon">&#xf007;</span><span class="label">12</span>' +
                '</div>' +
              '</li>' +
            '</ul>' +
            //'<div class="more-buckets">More...</div>' +
          '</div>').appendTo(parentEl),

        update = function(buckets) {
          /*
          var total = buckets.reduce(function(acc, bucket) {
                var count = bucket[Object.keys(bucket)[0]];
                return acc + count;
              }, 0),

              topThree = buckets.slice(0, 3).map(function(bucket) {
                var t = Object.keys(bucket)[0],
                    v = bucket[t];

                return { type: t, count: v };
              }),

              bottomHeight = (topThree.length < 2) ? 0 : topThree.slice(1,3).reduce(function(acc, b) {
                return acc + b.count;
              }, 0) / total,

              topHeight = 1 - bottomHeight,

              append = function(facet, width, height, minW, maxW, minH, maxH, buffer) {
                var span = jQuery('<span class="type-count"></span>'),

                    toPcnt = function(n, min, max) {
                      var pcnt = Math.round(100 * n) - buffer;
                      return Math.min(Math.max(min, pcnt), max) + '%';
                    };

                span.addClass(facet.type);
                span.html(Formatting.formatNumber(facet.count));
                span.css({
                  width: toPcnt(width, minW, maxW),
                  height: toPcnt(height, minH, maxH)
                });

                el.append(span);

                // Set fontsize once we know absolute dimensions
                span.css({
                  lineHeight: span.outerHeight() + 'px',
                  fontSize: Math.round(0.7 * span.outerHeight())+ 'px'
                });
              };

          el.empty();

          if (topThree.length > 0) {
            // First line - 1st facet count
            if (topThree.length === 1)
              // Only a single facet count
              append(topThree[0], 1, topHeight, 0, 100, 0, 100, 0);
            else
              // At least two facet counts
              append(topThree[0], 1, topHeight, 0, 100, 35, 65, 1.5);

            // Second line - 2nd and 3rd facet count
            if (topThree.length == 2) {
              // Two facet counts
              append(topThree[1], 1, bottomHeight, 0, 100, 35, 65, 1.5);
            } else if (topThree.length === 3) {
              // Three facet counts
              var w = topThree[1].count / (topThree[1].count + topThree[2].count);
              append(topThree[1], w, bottomHeight, 20, 80, 35, 65, 1.5);
              append(topThree[2], 1 - w, bottomHeight, 20, 80, 35, 65, 1.5);
            }
          } */
        };

    this.update = update;
  };

  return TypeFacet;

});
