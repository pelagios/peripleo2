define(['ui/common/formatting'], function(Formatting) {

  var TypeCounts = function(parentEl) {
    var el = jQuery('<ul></ul>').appendTo(parentEl),

        update = function(buckets) {
          el.empty();
          buckets.forEach(function(b) {
            var t = b.path[0].id;
            el.append('<li class="count ' + t + '">' + Formatting.formatNumber(b.count) + '</li>');
          });
        };

    this.update = update;
  };

  return TypeCounts;

});
