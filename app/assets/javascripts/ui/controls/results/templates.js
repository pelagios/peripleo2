define([
  'ui/common/formatting',
  'ui/common/itemUtils'
], function(Formatting, ItemUtils) {

  var BASE_TEMPLATE =
        '<li>' +
          '<div class="item-thumbnail"></div>' +
          '<div class="item-info">' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-temporal-bounds"></p>' +
            '<p class="item-in-dataset"></p>' +
          '</div>' +
        '</li>',

      baseTemplate = function(item) {
        var li = jQuery(BASE_TEMPLATE);

        li.find('.item-title').html(item.title);

        if (item.temporal_bounds)
          li.find('.item-temporal-bounds').html(Formatting.formatTemporalBounds(item.temporal_bounds));

        return li;
      },

      createPlaceRow = function(item) {
        return baseTemplate(item);
      },

      createObjectRow = function(item) {
        var li = baseTemplate(item),

            // We'll assume items to have a single record for now
            inDataset = ItemUtils.getHierarchyPath(item.is_conflation_of[0].is_in_dataset);

        // Only display top-level dataset
        li.find('.item-in-dataset').html(inDataset[0].title);
        return li;
      },

      createPersonRow = function(item) {
        return baseTemplate(item);
      },

      createDatasetRow = function(item) {
        return baseTemplate(item);
      };

  return {

    createRow : function(item) {
      switch(ItemUtils.getItemType(item)) {
        case 'PLACE':
          return createPlaceRow(item);
        case 'OBJECT':
          return createObjectRow(item);
        case 'PERSON':
          return createPersonRow(item);
        case 'DATASET':
          return createDatasetRow(item);
      }
    }

  };

});
