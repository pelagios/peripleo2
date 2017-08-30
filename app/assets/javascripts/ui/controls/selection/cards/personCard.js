define([
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/controls/selection/cards/baseCard'
], function(Formatting, ItemUtils, BaseCard) {

  var PersonCard  = function(parentEl, person, args) {

    var self = this,

        element = jQuery(
          '<div class="item-info">' +
            '<h3 class="item-title"></h3>' +
            '<ul class="item-identifiers"></ul>' +
            '<p class="item-temporal-bounds"></p>' +
            '<p class="item-description"></p>' +
          '</div>').appendTo(parentEl),

        title       = element.find('.item-title'),
        identifiers = element.find('.item-identifiers'),
        tempBounds  = element.find('.item-temporal-bounds'),
        description = element.find('.item-description'),

        references =
          jQuery('<div class="person references"><span class="icon">&#xf0c1;</span></div>').appendTo(parentEl),

        renderInfo = function() {
          var birthDate, deathDate;

          self.fill(title, person.title);
          self.renderIdentifiers(identifiers, ItemUtils.getURIs(person));

          if (person.temporal_bounds) {
            birthDate = Formatting.formatDate(person.temporal_bounds.from);
            deathDate = Formatting.formatDate(person.temporal_bounds.to);
            tempBounds.html(
              '<span class="person-birth">' + birthDate + '</span>' +
              '<span class="person-death">' + deathDate + '</span>');
          }

          self.fillDescription(description, person);
        },

        renderReferences = function() {
          self.renderInboundLinks(references, args.results);
        };

    BaseCard.apply(this);

    renderInfo();
    renderReferences();
  };
  PersonCard.prototype = Object.create(BaseCard.prototype);

  return PersonCard;

});
