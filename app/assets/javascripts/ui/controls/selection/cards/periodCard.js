define([
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/controls/selection/cards/baseCard'
], function(Formatting, ItemUtils, BaseCard) {

  var PeriodCard  = function(parentEl, period, args) {

    var self = this,

        element = jQuery(
          '<div class="item-info">' +
            '<p class="item-is-in"></p>' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-homepage"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        inDataset  = element.find('.item-is-in'),
        title      = element.find('.item-title'),
        homepage   = element.find('.item-homepage'),
        tempBounds = element.find('.item-temporal-bounds'),

        render = function() {
          // For the time being, periods only come from PeriodO, so one record only
          var record = period.is_conflation_of[0];
          self.renderHierarchyPath(inDataset, record.is_in_dataset);
          self.fill(title, period.title);
          self.fill(homepage, record.uri);
          self.fillIfExists(tempBounds, period.temporal_bounds);
        };

    BaseCard.apply(this);

    render();
  };
  PeriodCard.prototype = Object.create(BaseCard.prototype);

  return PeriodCard;

});
