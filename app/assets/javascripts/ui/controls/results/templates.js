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
            '<p class="item-description"></p>' +
            '<p class="item-temporal-bounds"></p>' +
            '<p class="item-is-in"></p>' +
          '</div>' +
        '</li>',

      ICON_PLACE = '<span class="icon stroke7">&#xe638;</span>',
      ICON_OBJECT = '<span class="icon stroke7">&#xe6af;</span>',
      ICON_PERSON = '<span class="icon tonicons">&#xe863;</span>',
      ICON_DATASET = '<span class="icon stroke7">&#xe674;</span>',

      baseTemplate = function(item) {
        var li = jQuery(BASE_TEMPLATE),
            descriptions = ItemUtils.getDescriptions(item);

        li.find('.item-title').html(item.title);

        if (descriptions.length > 0)
          li.find('.item-description').html(descriptions[0].description);

        if (item.temporal_bounds)
          li.find('.item-temporal-bounds').html(Formatting.formatTemporalBounds(item.temporal_bounds));

        return li;
      },

      createPlaceRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon'),

            placeIds = jQuery('<p class="place-ids-small"></p>').insertAfter(li.find('.item-title')),

            refs = ItemUtils.getURIs(item).map(function(uri) { return PlaceUtils.parseURI(uri); }),

            appendRefs = function(refs) {
              refs.forEach(function(ref) {
                if (ref.isKnownGazetteer) {
                  placeIds.append('<span class="place-id" title="' + ref.shortcode + ':' + ref.id +
                  '" style="background-color:' + ref.color + '">' + ref.initial + '</span>');
                } else {
                  placeIds.append('<span class="place-id" title="' + ref.uri + '">?</span>');
                }
              });
            };

        icon.addClass('place');
        icon.prop('title', 'Place');
        icon.html(ICON_PLACE);

        if (refs.length < 8) {
          appendRefs(refs);
        } else {
          appendRefs(refs.slice(1, 8));
          placeIds.append('<span class="place-more-ids">and ' + (refs.length - 8) + ' more...</span>');
        }

        return li;
      },

      createObjectRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon'),

            // We'll assume items to have a single record for now
            inDataset = ItemUtils.getHierarchyPath(item.is_conflation_of[0].is_in_dataset);

        // TODO use different icons, depending on item type
        icon.addClass('object');
        icon.prop('title', 'Object');
        icon.html(ICON_OBJECT);

        // Only display top-level dataset
        li.find('.item-is-in').html(inDataset[0].title);
        return li;
      },

      createPersonRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon');

        icon.addClass('person');
        icon.prop('title', 'Person');
        icon.html(ICON_PERSON);
        return li;
      },

      createDatasetRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon'),

            // We'll assume datasets to have a single record for now
            record = item.is_conflation_of[0],

            isPartOf = (record.is_part_of) ? ItemUtils.getHierarchyPath(record.is_part_of) : false;

        icon.addClass('dataset');
        icon.prop('title', 'Dataset');
        icon.html(ICON_DATASET);

        if (isPartOf)
          li.find('.item-is-in').html(isPartOf[0].title);

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
