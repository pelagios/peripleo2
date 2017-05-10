define([
  'ui/common/formatting',
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/api'], function(Formatting, HasEvents, ItemUtils, API) {

  var ObjectCard  = function(parentEl, item) {
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
          // TODO load indicator
          API.getReferences(record.uri).done(function(response) {
            var places = response.PLACE,
                head = (places && places.length > 3) ? places.slice(0, 3) : places;

            if (head) {
              head.forEach(function(place) {
                referencesEl.html(
                  '<p class="findspot">' + // TODO we will have more than just findspots in the future!
                    '<span><a href="#" title="' + place.description + '">' + place.title + '</a></span>' +
                  '</p>');
              });
            }
          });
        };

    HasEvents.apply(this);
    renderInfo();
    renderReferences();
  };
  ObjectCard.prototype = Object.create(HasEvents.prototype);

  return ObjectCard;

});
