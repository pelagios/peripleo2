define([
  'ui/common/formatting'
], function(Formatting) {

  var SourceFacet = function(parentEl) {

    var el = jQuery(
          '<div class="facet by-source right">' +
            '<div class="top-count">1,802</div>' +
            '<div class="top-source">' +
              'American Numismatic Society' +
            '</div>' +
            '<div class="more-buckets"><span class="icon">&#xf187;</span> 5 other sources</div>' +
          '</div>').appendTo(parentEl),

        update = function(buckets) {
          console.log(buckets);
        };

    this.update = update;
  };

  return SourceFacet;

});
