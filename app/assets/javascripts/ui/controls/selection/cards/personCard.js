define([
  'ui/common/formatting',
  'ui/common/itemUtils'
], function(Formatting, ItemUtils) {

  var PersonCard  = function(parentEl, person, args) {

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

        referencesEl =
          jQuery('<div class="person references"><span class="icon">&#xf0c1;</span></div>').appendTo(parentEl),

        render = function() {
          var identifiers = ItemUtils.getURIs(person).map(function(uri) { return ItemUtils.parseEntityURI(uri); }),
              descriptions = ItemUtils.getDescriptions(person).map(function(d) { return d.description; }),
              birthDate, deathDate;

          titleEl.html(person.title);

          identifiers.forEach(function(id) {
            var formatted = (id.shortcode) ? id.shortcode + ':' + id.id : id.uri,
                li = jQuery('<li><a href="' + id.uri + '" target="_blank">' + formatted + '</a></li>');

            if (id.color) li.css('background-color', id.color);
            identifiersEl.append(li);
          });

          if (person.temporal_bounds) {
            birthDate = Formatting.formatDate(person.temporal_bounds.from);
            deathDate = Formatting.formatDate(person.temporal_bounds.to);
            tempBoundsEl.html(
              '<span class="person-birth">' + birthDate + '</span>' +
              '<span class="person-death">' + deathDate + '</span>');
          }

          if (descriptions.length > 0)
            descriptionEl.html(descriptions[0]);
        },

        renderRelations = function() {
          if (args.results > 0) {
            refEl = jQuery(
              '<span class="inbound-links">' +
                '<a class="local-search" href="#">' + Formatting.formatNumber(args.results) + ' items</a> link to this person' +
              '</span>');

            refEl.find('a').data('at', person);

            referencesEl.append(refEl);
          } else {
            referencesEl.append('No items link here');
          }
        };

    render();
    renderRelations();
  };

  return PersonCard;

});
