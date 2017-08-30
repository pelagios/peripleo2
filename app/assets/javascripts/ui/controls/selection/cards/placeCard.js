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
            '<p class="item-names"></p>' +
            '<p class="item-temporal-bounds"></p>' +
            '<p class="item-description"></p>' +
            '<div class="item-identifiers">' +
              '<ul></ul>' +
              '<a href="#" class="ld-view-link">&#xe616;<a>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        title       = element.find('.item-title'),
        identifiers = element.find('.item-identifiers ul'),
        names       = element.find('.item-names'),
        description = element.find('.item-description'),
        tempBounds  = element.find('.item-temporal-bounds'),

        refRel =
          jQuery('<div class="place references"><span class="icon">&#xf0c1;</span></div>').appendTo(parentEl),

        renderInfo = function() {
          // We'll split 'names' that are actually lists of comma-separated names as well
          var distinctNames = distinct(ItemUtils.getNames(place).reduce(function(all, n) {
                return all.concat(n.name.split(',').map(function(n) { return n.trim(); }));
              }, []));

          self.fill(title, place.title);
          if (distinctNames.length > 12)
            self.fill(names, distinctNames.slice(0,12).join(', ') + ',...');
          else
            self.fill(names, distinctNames.join(', '));
          self.fillTemporalBounds(tempBounds, place.temporal_bounds);
          self.fillDescription(description, place);
          self.renderIdentifiers(identifiers, ItemUtils.getURIs(place));
        },

        /**
         * Renders information about
         * - the no. of items referencing this place
         * - the no. of other places related to this place
         */
        renderConnected = function() {
          self.renderInboundLinks(refRel, args.referencingCount);

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
