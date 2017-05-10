define([
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/common/placeUtils'
], function(Formatting, ItemUtils, PlaceUtils) {

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
        var li = baseTemplate(item),

            inDatasetEl = li.find('.item-in-dataset'),

            gazetteerRefs = ItemUtils.getURIs(item).map(function(uri) {
              return PlaceUtils.parseURI(uri);
            }),

            formatRef = function(ref) {
              if (ref.shortcode)
                return '<a class="place-minilink" href="' + ref.uri + '" title="' +
                  ref.shortcode + ':' + ref.id + '" style="background-color:' +
                  ref.color + '" target="_blank">' + ref.initial + '</a>';
              else
                // Really shouldn't happen in practice since it looks ugly
                return '<a class="place-minilink" href="' + ref.uri + '" title="' +
                  ref.uri + '" target="_blank">?</a>';
            };

        gazetteerRefs.forEach(function(ref) {
          inDatasetEl.append(formatRef(ref));
        });

        return li;
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
