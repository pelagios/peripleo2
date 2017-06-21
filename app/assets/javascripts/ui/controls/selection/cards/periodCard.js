define([
  'ui/common/formatting'
], function(Formatting) {

  var PeriodCard  = function(parentEl, period, args) {

    var infoEl = jQuery(
          '<div class="item-info">' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-homepage"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

          titleEl      = infoEl.find('.item-title'),
          homepageEl   = infoEl.find('.item-homepage'),
          tempBoundsEl = infoEl.find('.item-temporal-bounds'),

          render = function() {
            titleEl.html(period.title);
            homepageEl.html(period.is_conflation_of[0].uri);

            if (period.temporal_bounds)
              tempBoundsEl.html(Formatting.formatTemporalBounds(period.temporal_bounds));
          };

    render();
  };

  return PeriodCard;

});
