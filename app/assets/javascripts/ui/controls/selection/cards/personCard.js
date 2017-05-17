define([
  'ui/common/formatting',
  'ui/common/hasEvents',
  'ui/common/itemUtils'
], function(Formatting, HasEvents, ItemUtils) {

  var PersonCard  = function(parentEl, person, references) {
    var infoEl = jQuery(
          '<div class="item-info">' +
            '<h3 class="item-title"></h3>' +
            '<ul class="item-identifiers"></ul>' +
            '<p class="item-names"></p>' +
            '<p class="item-temporal-bounds"></p>' +
            '<p class="item-description"></p>' +
          '</div>').appendTo(parentEl),

        titleEl      = infoEl.find('.item-title'),
        identifiersEl = infoEl.find('.item-identifiers'),
        namesEl       = infoEl.find('.item-names'),
        tempBoundsEl = infoEl.find('.item-temporal-bounds'),
        descriptionEl = infoEl.find('.item-description'),

        render = function() {
          var descriptions = ItemUtils.getDescriptions(person).map(function(d) { return d.description; }),
              birthDate, deathDate;

          titleEl.html(person.title);

          if (person.temporal_bounds) {
            birthDate = Formatting.formatDate(person.temporal_bounds.from);
            deathDate = Formatting.formatDate(person.temporal_bounds.to);
            tempBoundsEl.html(
              '<span class="person-birth">' + birthDate + '</span>' +
              '<span class="person-death">' + deathDate + '</span>');
          }

          if (descriptions.length > 0)
            descriptionEl.html(descriptions[0]);
        };

    HasEvents.apply(this);

    render();
  };
  PersonCard.prototype = Object.create(HasEvents.prototype);

  return PersonCard;

});
