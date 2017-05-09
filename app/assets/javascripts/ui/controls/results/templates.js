define(['ui/common/itemUtils'], function(ItemUtils) {

  var BASE_TEMPLATE =
        '<li>' +
          '<h3></h3>' +
          '<p class="temporal-bounds"></p>' +
          '<p class="in-dataset"></p>' +
        '</li>',

      baseTemplate = function(item) {
        var li = jQuery(BASE_TEMPLATE);

        li.find('h3').html(item.title);
        
        // li.find('.in-dataset').html('foo');

        // if (temporalBounds)
        //   li.find('.temporal-bounds').html(Formatting.formatTemporalBounds(temporalBounds));

        return li;
      },

      createPlaceRow = function(item) {
        return baseTemplate(item);
      },

      createObjectRow = function(item) {
        return baseTemplate(item);
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
