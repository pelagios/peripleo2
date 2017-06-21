define([
  'ui/common/formatting',
  'ui/common/itemUtils'
], function(Formatting, ItemUtils) {

  var PeriodCard  = function(parentEl, period, args) {

    var infoEl = jQuery(
          '<div class="item-info">' +
            '<p class="item-is-in"></p>' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-homepage"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

          inDatasetEl  = infoEl.find('.item-is-in'),
          titleEl      = infoEl.find('.item-title'),
          homepageEl   = infoEl.find('.item-homepage'),
          tempBoundsEl = infoEl.find('.item-temporal-bounds'),

          render = function() {
            // For the time being, periods only come from PeriodO, so one record only
            var record = period.is_conflation_of[0];

            ItemUtils.getHierarchyPath(record.is_in_dataset).forEach(function(segment) {
              inDatasetEl.append(
                '<span><a class="destination" data-id="' + segment.id + '" href="#">' +
                  segment.title +
                '</a></span>');
            });

            titleEl.html(period.title);
            homepageEl.html(record.uri);

            if (period.temporal_bounds)
              tempBoundsEl.html(Formatting.formatTemporalBounds(period.temporal_bounds));
          };

    render();
  };

  return PeriodCard;

});
