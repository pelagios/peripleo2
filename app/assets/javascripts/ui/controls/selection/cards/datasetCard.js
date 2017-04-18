define([
  'ui/common/itemUtils',
  'ui/controls/selection/cards/card'
], function(ItemUtils, Card) {

  var DatasetCard  = function(parentEl, dataset) {
    var self = this,

        infoEl = jQuery(
          '<div class="info">' +
            '<p class="part-of"></p>' +
            '<h3></h3>' +
            '<p class="description"></p>' +
          '</div>').appendTo(parentEl),

        partOfEl      = infoEl.find('.part-of'),
        titleEl       = infoEl.find('h3'),
        descriptionEl = infoEl.find('.description'),

        // TODO safe to assume that datasets only have one record
        record = dataset.is_conflation_of[0],

        render = function() {
          var descriptions = self.getDescriptions(dataset);

          if (record.is_part_of)
            ItemUtils.getHierarchyPath(record.is_part_of).forEach(function(segment) {
              partOfEl.append('<span>' +
                '<a class="destination" data-id="' + segment.id + '" href="#">' + segment.title +
                '</a></span>');
            });

          titleEl.html(dataset.title);
          if (descriptions.length > 0)
            descriptionEl.html(descriptions[0].description);
        };

    Card.apply(this);
    render();
  };
  DatasetCard.prototype = Object.create(Card.prototype);

  return DatasetCard;

});
