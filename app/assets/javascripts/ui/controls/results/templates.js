define([
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/common/placeUtils'
], function(Formatting, ItemUtils, PlaceUtils) {

  var BASE_TEMPLATE =
        '<li>' +
          '<div class="item-icon"></div>' +
          '<div class="item-info">' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-temporal-bounds"></p>' +
            '<p class="item-in-dataset"></p>' +
          '</div>' +
        '</li>',

      ICON_PLACE = '<span class="icon stroke7">&#xe638;</span>',
      ICON_OBJECT = '<span class="icon stroke7">&#xe6af;</span>',
      ICON_PERSON = '<span class="icon tonicons">&#xe863;</span>',
      ICON_DATASET = '<span class="icon stroke7">&#xe674;</span>',

      baseTemplate = function(item) {
        var li = jQuery(BASE_TEMPLATE);

        li.find('.item-title').html(item.title);

        if (item.temporal_bounds)
          li.find('.item-temporal-bounds').html(Formatting.formatTemporalBounds(item.temporal_bounds));

        return li;
      },

      createPlaceRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon'),

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

        icon.addClass('place');
        icon.html(ICON_PLACE);

        gazetteerRefs.forEach(function(ref) {
          inDatasetEl.append(formatRef(ref));
        });

        return li;
      },

      createObjectRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon'),

            // We'll assume items to have a single record for now
            inDataset = ItemUtils.getHierarchyPath(item.is_conflation_of[0].is_in_dataset);

        // TODO use different icons, depending on item type
        icon.addClass('object');
        icon.html(ICON_OBJECT);

        // Only display top-level dataset
        li.find('.item-in-dataset').html(inDataset[0].title);
        return li;
      },

      createPersonRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon');

        icon.addClass('person');
        icon.html(ICON_PERSON);
        return li;
      },

      createDatasetRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon');

        icon.addClass('dataset');
        icon.html(ICON_DATASET);
        return li;
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
