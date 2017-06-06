define([], function() {

  var FacetsOverview = function(parentEl) {

    var el = jQuery(
          '<div class="facets-overview">' +
            '<div class="facets-row">' +
              '<div class="facet sources">' +
                '<span class="icon">&#xf187;</span>' +
                '<span class="value"><span class="count">5</span> sources</span>' +
              '</div>' +

              '<div class="facet categories">' +
                '<span class="icon">&#xf02b;</span>' +
                '<span class="value"><span class="count">7</span> topics</span>' +
              '</div>' +

              '<div class="facet people">' +
                '<span class="icon">&#xf007;</span>' +
                '<span class="value"><span class="count">0</span> people</span>' +
              '</div>' +

              '<div class="facet periods">' +
                '<span class="icon">&#xf017;</span>' +
                '<span class="value"><span class="count">0</span> periods</span>' +
              '</div>' +
            '</div>' +

            '<div class="types-divider">' +
              '<div class="clickbuffer"></div>' +
              '<div class="bar"></div>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        count = el.find('.count'),
        typeDivider = el.find('.type-divider'),
        facetsRow = el.find('.facets-row'),

        update = function(response) {

        };

    this.update = update;
  };

  return FacetsOverview;

});
