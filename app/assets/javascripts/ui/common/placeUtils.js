define(function() {

  // TODO fetch this information from the server, so we can feed it from the DB
  var KNOWN_GAZETTEERS = [
        { shortcode: 'pleiades', initial: 'P', url_patterns: [ 'http://pleiades.stoa.org/places/' ], color: '#1f77b4' },
        { shortcode: 'dare',     initial: 'D', url_patterns: [ 'http://dare.ht.lu.se/places/' ],     color: '#ff7f0e' },
        { shortcode: 'geonames', initial: 'G', url_patterns: ['http://sws.geonames.org/'],           color: '#2ca02c' }
      ];

  return {

    /**
     * Parses a gazetteer URI and determines the appropriate gazetteer
     * shortcode, ID, and signature color.
     */
    parseURI : function(uri) {
      var parseResult = { uri: uri };

      jQuery.each(KNOWN_GAZETTEERS, function(i, g) {
        var cont = true;
        jQuery.each(g.url_patterns, function(j, pattern) {
          if (uri.indexOf(pattern) === 0) {
            parseResult.shortcode = g.shortcode;
            parseResult.id = uri.substring(pattern.length);
            parseResult.color = g.color;
            parseResult.initial = g.initial;

            cont = false;
            return cont;
          }
        });
        return cont;
      });

      return parseResult;
    }

  };

});
