define([
  'ui/common/formatting',
  'ui/common/itemUtils'], function(Formatting, ItemUtils) {

  var ObjectCard  = function(parentEl, item, references, resultCounts) {
    var infoEl = jQuery(
          '<div class="item-info">' +
            '<p class="item-is-in"></p>' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-homepage"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        inDatasetEl  = infoEl.find('.item-is-in'),
        titleEl      = infoEl.find('.item-title'),
        homepageEl   = infoEl.find('.item-homepage'),
        tempBoundsEl = infoEl.find('.item-temporal-bounds'),

        referencesEl = jQuery(
          '<div class="item-references"></div>').appendTo(parentEl),

        // TODO we'll assume that objects only have one record for now
        record = item.is_conflation_of[0],

        renderInfo = function() {
          var title = (record.homepage) ?
                '<a href="' + record.homepage + '" target="_blank">' + item.title + '</a>' :
                item.title;

          ItemUtils.getHierarchyPath(record.is_in_dataset).forEach(function(segment) {
            inDatasetEl.append('<span><a href="#">' + segment.title + '</a></span>');
          });

          titleEl.html(title);
          if (record.homepage)
            homepageEl.html(record.homepage);
          if (item.temporal_bounds)
            tempBoundsEl.html(Formatting.formatTemporalBounds(item.temporal_bounds));
        },

        renderReferences = function() {

          var places = references.PLACE,
              head = (places && places.length > 3) ? places.slice(0, 3) : places;

          if (head) {
            head.forEach(function(place) {
              var counts = jQuery.grep(resultCounts, function(r) {
                    return place.identifiers.indexOf(r.identifier) > -1;
                  }),

                  count = (counts.length > 0) ? counts[0].resultCount : 0,

                  label = (count > 2) ? 'results' : 'result',

                  moreResults = (count > 1) ?
                    '<span class="more"> · <a href="#">' + Formatting.formatNumber(count - 1) + ' more ' + label + '</a></span>' :
                    '';

              referencesEl.append(
                '<p class="ref-place">' +
                  '<span class="title"><a href="#">' +
                    place.title +
                  '</a></span>' + moreResults +
                '</p>');
            });
          }
        };

    renderInfo();
    renderReferences();
  };

  return ObjectCard;

});
