define([
  'ui/common/formatting',
  'ui/common/itemUtils'
], function(Formatting, ItemUtils) {

  var BASE_TEMPLATE =
        '<li>' +
          '<div class="item-color"></div>' +
          '<div class="item-info">' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-description"></p>' +
            '<p class="item-temporal-bounds"></p>' +
            '<p class="item-is-in"></p>' +
          '</div>' +
        '</li>',

      /** Fills the common basics: title, description, temporal bounds **/
      renderBase = function(item) {
        var li = jQuery(BASE_TEMPLATE),
            descriptions = ItemUtils.getDescriptions(item);

        li.find('.item-title').html(item.title);
        if (descriptions.length > 0)
          li.find('.item-description').html(descriptions[0].description);
        if (item.temporal_bounds)
          li.find('.item-temporal-bounds').html(Formatting.formatTemporalBounds(item.temporal_bounds));

        return li;
      },

      /** Renders the identifer bubbles on places and persons **/
      renderIdentifiers = function(el, item) {
        var identifiers = ItemUtils.getURIs(item).sort().map(function(uri) {
              return ItemUtils.parseEntityURI(uri);
            }),

            render = function(ids) {
              ids.forEach(function(id) {
                if (id.isKnownAuthority)
                  el.append(
                    '<span class="item-id ' + id.shortcode + '" title="' + id.shortcode + ':' + id.id +
                      '" style="background-color:' + id.color + '">' + id.initial +
                    '</span>');
                else
                  el.append('<span class="item-id" title="' + id.uri + '">?</span>');
              });
            };

        // Append max. 8 bubbles, plus a "more" notification if needed
        if (identifiers.length < 8) {
          render(identifiers);
        } else {
          render(identifiers.slice(1, 8));
          el.append('<span class="item-more-ids">and ' + (identifiers.length - 8) + ' more...</span>');
        }
      },

      createObjectRow = function(item) {
        var li = renderBase(item),

            // We'll assume items to have a single record for now
            inDataset = ItemUtils.getHierarchyPath(item.is_conflation_of[0].is_in_dataset);

        li.addClass('object');

        // Only display top-level dataset
        li.find('.item-is-in').html(inDataset[0].title);
        return li;
      },

      createPlaceRow = function(item) {
        var li = renderBase(item),
            identifiersEl = jQuery('<p class="item-identifiers-small"></p>').insertAfter(li.find('.item-title'));

        li.addClass('place');
        renderIdentifiers(identifiersEl, item);
        return li;
      },

      createPersonRow = function(item) {
        var li = renderBase(item),
            identifiersEl = jQuery('<p class="item-identifiers-small"></p>').insertAfter(li.find('.item-title'));

        li.addClass('person');
        renderIdentifiers(identifiersEl, item);
        return li;
      },

      createPeriodRow = function(item) {
        var li = renderBase(item);
        li.addClass('period');
        return li;
      },

      createDatasetRow = function(item) {
        var li = renderBase(item),

            // We'll assume datasets to have a single record for now
            record = item.is_conflation_of[0],

            isPartOf = (record.is_part_of) ? ItemUtils.getHierarchyPath(record.is_part_of) : false;

        li.addClass('dataset');

        if (isPartOf)
          li.find('.item-is-in').html(isPartOf[0].title);

        return li;
      };

  return {

    createRow : function(item) {
      switch(ItemUtils.getItemType(item)) {
        case 'OBJECT':
          return createObjectRow(item);
        case 'PLACE':
          return createPlaceRow(item);
        case 'PERSON':
          return createPersonRow(item);
        case 'PERIOD':
          return createPeriodRow(item);
        case 'DATASET':
          return createDatasetRow(item);
      }
    }

  };

});
