define(function() {

  // TODO fetch this information from the server, so we can feed it from the DB
  var KNOWN_GAZETTEERS = [
        { shortcode: 'pleiades', initial: 'P', color: '#5b9ec4', url_patterns: [ 'http://pleiades.stoa.org/places/' ] },
        { shortcode: 'dare',     initial: 'D', color: '#9e9ac8', url_patterns: [ 'http://dare.ht.lu.se/places/' ] },
        { shortcode: 'geonames', initial: 'G', color: '#74c476', url_patterns: ['http://sws.geonames.org/'] }
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
            parseResult.id = uri.substring(pattern.length);
            parseResult.initial = g.initial;
            parseResult.color = g.color;
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
