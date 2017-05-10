define([
  'ui/common/formatting',
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/common/placeUtils'
], function(Formatting, HasEvents, ItemUtils, PlaceUtils) {

  var distinct = function(arr) {
        return arr.reduce(function(distinct, elem) {
          if (distinct.indexOf(elem) < 0) distinct.push(elem);
          return distinct;
        }, []);
      };

  var PlaceCard  = function(parentEl, place) {
    var infoEl = jQuery(
          '<div class="item-info">' +
            '<h3 class="item-title"></h3>' +
            '<ul class="place-identifiers"></ul>' +
            '<p class="place-names"></p>' +
            '<p class="item-description"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        titleEl       = infoEl.find('.item-title'),
        identifiersEl = infoEl.find('.place-identifiers'),
        namesEl       = infoEl.find('.place-names'),
        descriptionEl = infoEl.find('.item-description'),
        tempBoundsEl  = infoEl.find('.item-temporal-bounds'),

        render = function() {
          var identifiers = ItemUtils.getURIs(place).map(function(uri) { return PlaceUtils.parseURI(uri); }),
              descriptions = ItemUtils.getDescriptions(place).map(function(d) { return d.description; }),
              names = distinct(ItemUtils.getNames(place).map(function(n) { return n.name; }));

          titleEl.html(place.title);

          identifiers.forEach(function(id) {
            var formatted = (id.shortcode) ? id.shortcode + ':' + id.id : id.uri,
                li = jQuery('<li><a href="' + id.uri + '" target="_blank">' + formatted + '</a></li>');

            if (id.color) li.css('background-color', id.color);
            identifiersEl.append(li);
          });

          if (descriptions.length > 0)
            descriptionEl.html(descriptions[0]);

          namesEl.html(names.join(', '));

          if (place.temporal_bounds)
            tempBoundsEl.html(Formatting.formatTemporalBounds(place.temporal_bounds));
        };

    HasEvents.apply(this);
    render();
  };
  PlaceCard.prototype = Object.create(HasEvents.prototype);

  return PlaceCard;

});
