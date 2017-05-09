define([
  'ui/common/hasEvents',
  'ui/common/itemUtils'
], function(HasEvents, ItemUtils) {

  var DatasetCard  = function(parentEl, dataset) {
    var infoEl = jQuery(
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

    HasEvents.apply(this);
    render();
  };
  DatasetCard.prototype = Object.create(HasEvents.prototype);

  return DatasetCard;

});
