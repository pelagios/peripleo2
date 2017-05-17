define([
  'ui/common/formatting',
  'ui/common/itemUtils'
], function(Formatting, ItemUtils) {

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

      /** Code for rendering the identifer bubbles on places and persons **/
      appendIdentifiers = function(el, item) {
        var identifiers = ItemUtils.getURIs(item).map(function(uri) { return ItemUtils.parseEntityURI(uri); }),

            append = function(ids) {
              ids.forEach(function(id) {
                if (id.isKnownAuthority) {
                  el.append('<span class="item-id ' + id.shortcode + '" title="' + id.shortcode + ':' + id.id +
                    '" style="background-color:' + id.color + '">' + id.initial + '</span>');
                } else {
                  el.append('<span class="item-id" title="' + id.uri + '">?</span>');
                }
              });
            };

        if (identifiers.length < 8) {
          append(identifiers);
        } else {
          append(identifiers.slice(1, 8));
          el.append('<span class="item-more-ids">and ' + (identifiers.length - 8) + ' more...</span>');
        }
      },

      createPlaceRow = function(item) {
        var li = baseTemplate(item),
            icon = li.find('.item-icon'),
            identifiersEl = jQuery('<p class="item-identifiers-small"></p>').insertAfter(li.find('.item-title'));

        icon.addClass('place');
        icon.prop('title', 'Place');
        icon.html(ICON_PLACE);
        appendIdentifiers(identifiersEl, item);
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
            icon = li.find('.item-icon'),
            identifiersEl = jQuery('<p class="item-identifiers-small"></p>').insertAfter(li.find('.item-title'));

        icon.addClass('person');
        icon.prop('title', 'Person');
        icon.html(ICON_PERSON);
        appendIdentifiers(identifiersEl, item);
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
