define([], function() {

  var PeopleFacet = function(parentEl) {
    var el = jQuery(
          '<div class="facet people left bottom">' +
            '<div class="top-related">No related people</div>' +
          '</div>').appendTo(parentEl);
  };

  return PeopleFacet;

});
