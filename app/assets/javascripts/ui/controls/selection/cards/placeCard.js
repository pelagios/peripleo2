define(['ui/controls/selection/cards/card'], function(Card) {

  // TODO fetch this information from the server, so we can feed it from the DB
  var KNOWN_GAZETTEERS = [
        { shortcode: 'pleiades', url_patterns: [ 'http://pleiades.stoa.org/places/' ], color: '#1f77b4' },
        { shortcode: 'dare', url_patterns: [ 'http://dare.ht.lu.se/places/' ], color: '#ff7f0e' },
        { shortcode: 'geonames', url_patterns: ['http://sws.geonames.org/'], color: '#2ca02c' }
      ],

      /**
       * Parses a gazetteer URI and determines the appropriate gazetteer
       * shortcode, ID, and signature color.
       */
      parseURI = function(uri) {
        var parseResult = { uri: uri };

        jQuery.each(KNOWN_GAZETTEERS, function(i, g) {
          var cont = true;
          jQuery.each(g.url_patterns, function(j, pattern) {
            if (uri.indexOf(pattern) === 0) {
              parseResult.shortcode = g.shortcode;
              parseResult.id = uri.substring(pattern.length);
              parseResult.color = g.color;

              cont = false;
              return cont;
            }
          });
          return cont;
        });

        return parseResult;
      };

  var PlaceCard  = function(parentEl, place) {
    var self = this,

        infoEl = jQuery(
          '<div class="info">' +
            '<h3></h3>' +
            '<ul class="identifiers"></ul>' +
            '<p class="description"></p>' +
            '<p class="names"></p>' +
            '<p class="temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        titleEl       = infoEl.find('h3'),
        identifiersEl = infoEl.find('.identifiers'),
        descriptionEl = infoEl.find('.description'),
        namesEl       = infoEl.find('.names'),
        tempBoundsEl  = infoEl.find('.temporal-bounds'),

        render = function() {
          var identifiers = self.getURIs(place).map(function(uri) { return parseURI(uri); }),
              descriptions = self.getDescriptions(place).map(function(d) { return d.description; }),
              names = self.getNames(place).map(function(n) { return n.name; });

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
            tempBoundsEl.html(self.formatTemporalBounds(place.temporal_bounds));
        };

    Card.apply(this);

    render();
  };
  PlaceCard.prototype = Object.create(Card.prototype);

  return PlaceCard;

});
