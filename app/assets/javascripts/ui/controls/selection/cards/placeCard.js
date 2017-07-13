define([
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/controls/selection/cards/baseCard'
], function(Formatting, ItemUtils, BaseCard) {

  var distinct = function(arr) {
        return arr.reduce(function(distinct, elem) {
          if (distinct.indexOf(elem) < 0) distinct.push(elem);
          return distinct;
        }, []);
      };

  var PlaceCard  = function(parentEl, place, args) {

    var self = this,

        element = jQuery(
          '<div class="item-info">' +
            '<h3 class="item-title"></h3>' +
            '<ul class="item-identifiers"></ul>' +
            '<p class="place-names"></p>' +
            '<p class="item-description"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        title       = element.find('.item-title'),
        identifiers = element.find('.item-identifiers'),
        names       = element.find('.place-names'),
        description = element.find('.item-description'),
        tempBounds  = element.find('.item-temporal-bounds'),

        references =
          jQuery('<div class="place references"><span class="icon">&#xf0c1;</span></div>').appendTo(parentEl),

        renderInfo = function() {
          self.fill(title, place.title);
          self.renderIdentifiers(identifiers, ItemUtils.getURIs(place));
          self.fillWithFirst(description,
            ItemUtils.getDescriptions(place).map(function(d) { return d.description; }));
          self.fill(names,
            distinct(ItemUtils.getNames(place).map(function(n) { return n.name; })));
          self.fillTemporalBounds(place.temporal_bounds);
        },

        renderReferences = function() {
          if (args.results > 0) {
            var ref = jQuery(
              '<span class="inbound-links">' +
                '<a class="local-search" href="#">' + Formatting.formatNumber(args.results) + ' items</a> link here' +
              '</span>');

            ref.find('a').data('at', place);
            references.append(ref);
          } else {
            references.append('No items link here');
          }

          // TODO render related place count
          // references.append(
          // '·<span class="related-entities">' +
          //    '<span class="icon">&#xf140;</span>' +
          //     '<a href="#">' + related.length + ' related places</a>' +
          // '</span>');
        };

    BaseCard.apply(this);

    renderInfo();
    renderReferences();
  };
  PlaceCard.prototype = Object.create(BaseCard.prototype);

  return PlaceCard;

});
