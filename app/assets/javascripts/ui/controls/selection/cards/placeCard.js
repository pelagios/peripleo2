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
            '<p class="item-names"></p>' +
            '<p class="item-description"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        title       = element.find('.item-title'),
        identifiers = element.find('.item-identifiers'),
        names       = element.find('.item-names'),
        description = element.find('.item-description'),
        tempBounds  = element.find('.item-temporal-bounds'),

        refRel =
          jQuery('<div class="place references"><span class="icon">&#xf0c1;</span></div>').appendTo(parentEl),

        renderInfo = function() {
          self.fill(title, place.title);
          self.renderIdentifiers(identifiers, ItemUtils.getURIs(place));
          self.fillWithFirst(description,
            ItemUtils.getDescriptions(place).map(function(d) { return d.description; }));
          self.fill(names,
            distinct(ItemUtils.getNames(place).map(function(n) { return n.name; })).join(', '));
          self.fillTemporalBounds(place.temporal_bounds);
        },

        /**
         * Renders information about
         * - the no. of items referencing this place
         * - the no. of other places related to this place
         */
        renderConnected = function() {
          self.renderInboundLinks(refRel, place, args.referencingCount);

          if (args.relatedPlaces.length > 0)
            refRel.append(
              '·<span class="related-entities">' +
               '<span class="icon">&#xf140;</span>' +
                 '<a href="#">' + args.relatedPlaces.length + ' related places</a>' +
               '</span>');
        };

    BaseCard.apply(this);

    renderInfo();
    renderConnected();
  };
  PlaceCard.prototype = Object.create(BaseCard.prototype);

  return PlaceCard;

});
