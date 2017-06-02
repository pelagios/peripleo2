define([], function() {

  var PeriodFacet = function(parentEl) {
    var el = jQuery(
          '<div class="facet periods right bottom">' +
            '<div class="top-related">No related periods</div>' +
          '</div>').appendTo(parentEl);
  };

  return PeriodFacet;

});
