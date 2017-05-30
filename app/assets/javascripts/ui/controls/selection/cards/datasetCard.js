define([
  'ui/common/itemUtils'
], function(ItemUtils) {

  var DatasetCard  = function(parentEl, dataset, args) {
    var infoEl = jQuery(
          '<div class="item-info">' +
            '<p class="item-is-in"></p>' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-description"></p>' +
          '</div>').appendTo(parentEl),

        partOfEl      = infoEl.find('.item-is-in'),
        titleEl       = infoEl.find('.item-title'),
        descriptionEl = infoEl.find('.item-description'),

        // TODO safe to assume that datasets only have one record
        record = dataset.is_conflation_of[0],

        render = function() {
          var descriptions = ItemUtils.getDescriptions(dataset);

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

    render();
  };

  return DatasetCard;

});
