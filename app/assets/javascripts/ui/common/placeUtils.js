define(function() {

  // TODO fetch this information from the server, so we can feed it from the DB
  var KNOWN_GAZETTEERS = [
        { shortcode: 'pleiades', initial: 'P', color: '#1f77b4', url_patterns: [ 'http://pleiades.stoa.org/places/' ] },
        { shortcode: 'dare',     initial: 'D', color: '#ff7f0e', url_patterns: [ 'http://dare.ht.lu.se/places/' ] },
        { shortcode: 'geonames', initial: 'G', color: '#2ca02c', url_patterns: ['http://sws.geonames.org/'] }
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
            parseResult.isKnownGazetteer = true;
            parseResult.shortcode = g.shortcode;
            parseResult.initial = g.initial;
            parseResult.color = g.color;
            parseResult.id = uri.substring(pattern.length);
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
