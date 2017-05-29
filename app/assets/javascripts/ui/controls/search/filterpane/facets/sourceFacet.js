define([
  'ui/common/formatting'
], function(Formatting) {

  var SourceFacet = function(parentEl) {

    var el = jQuery(
          '<div class="facet by-source right">' +
            '<div class="top-count"></div>' +
            '<div class="top-source"></div>' +
            '<div class="more-buckets"></div>' + // '<span class="icon">&#xf187;</span> 5 other sources</div>' +
          '</div>').appendTo(parentEl),

        topCount = el.find('.top-count'),

        topSource = el.find('.top-source'),

        update = function(buckets) {
          var top = buckets[0],

              source = Object.keys(top)[0],
              sourcePaths = source.split('\u0007\u0007'),
              longestPath = sourcePaths[sourcePaths.length - 1].split('\u0007'),

              count = top[source];

          console.log(longestPath[1]);

          topCount.html(Formatting.formatNumber(count));
          topSource.html(longestPath[1]);
        };

    this.update = update;
  };

  return SourceFacet;

});
