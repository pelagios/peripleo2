define(function() {

  // TODO fetch this information from the server, so we can feed it from the DB
  var KNOWN_AUTHORITIES = [
        // Places
        { shortcode: 'beta',        initial: 'B', color: '#843c39', url_patterns: [ 'http://betamasaheft.eu/places/' ] },
        { shortcode: 'chgis',       initial: 'C', color: '#9467bd', url_patterns: [ 'http://maps.cga.harvard.edu/tgaz/placename/hvd_' ] },
        { shortcode: 'dare',        initial: 'D', color: '#9e9ac8', url_patterns: [ 'http://dare.ht.lu.se/places/' ] },
        { shortcode: 'defc',        initial: 'D', color: '#7f7f7f', url_patterns: [ 'http://defc.acdh.oeaw.ac.at/publicrecords/site/detail/' ] },
        { shortcode: 'geonames',    initial: 'G', color: '#74c476', url_patterns: [ 'http://sws.geonames.org/' ] },
        { shortcode: 'hgc',         initial: 'C', color: '#17becf', url_patterns: [ 'http://www.cyprusgazetteer.org/' ] },
        { shortcode: 'hgl',         initial: 'L', color: '#bcbd22', url_patterns: [ 'http://www.slsgazetteer.org/' ] },
        { shortcode: 'idai',        initial: 'i', color: '#ad494a', url_patterns: [ 'http://gazetteer.dainst.org/place/' ] },
        { shortcode: 'moeml',       initial: 'M', color: '#6b6ecf', url_patterns: [ 'http://mapoflondon.uvic.ca/' ] },
        { shortcode: 'nomisma',     initial: 'N', color: '#ff7f0e', url_patterns: [ 'http://nomisma.org/id/' ] },
        { shortcode: 'opencontext', initial: 'O', color: '#8c6d31', url_patterns: [ 'http://opencontext.org/subjects/' ] },
        { shortcode: 'pleiades',    initial: 'P', color: '#5b9ec4', url_patterns: [ 'http://pleiades.stoa.org/places/' ] },
        { shortcode: 'syriaca',     initial: 'S', color: '#8ca252', url_patterns: [ 'http://syriaca.org/place/' ] },
        { shortcode: 'tt',          initial: 't', color: '#e7ba52', url_patterns: [ 'http://topostext.org/place/' ] },
        { shortcode: 'rs',          initial: 'R', color: '#843c39', url_patterns: [ 'http://www.rusafricum.org/en/' ] },
        { shortcode: 'vici',        initial: 'V', color: '#d6616b', url_patterns: [ 'http://vici.org/vici/' ] },
        { shortcode: 'rrg',         initial: 'R', color: '#e7ba52', url_patterns: [ 'http://romeresearchgroup.org/items/' ] },
        { shortcode: 'courtauld',   initial: 'C', color: '#9037f0', url_patterns: [ 'http://sites.courtauld.ac.uk/crossingfrontiers/crossing-frontiers/'] }
      ],

      /**
       * Helper to map the list of conflated records to a list values of the given
       * record property. (E.g. go from list of records to list of descriptions.)
       */
      mapConflated = function(key) {
        return function(item) {
          var mapped = [];
          item.is_conflation_of.map(function(record) {
            var values = record[key];
            if (values)
              if (jQuery.isArray(values))
                mapped = mapped.concat(values);
              else
                mapped.push(values);
          });

          return mapped;
        };
      };

  return {

    /**
     * Parses an entity URI (place, person, etc.) and determines the appropriate authority ID
     * shortcode and signature color.
     */
    parseEntityURI : function(uri) {
      var parseResult = { uri: uri };

      jQuery.each(KNOWN_AUTHORITIES, function(i, auth) {
        var cont = true;
        jQuery.each(auth.url_patterns, function(j, pattern) {
          if (uri.indexOf(pattern) === 0) {
            parseResult.isKnownAuthority = true;
            parseResult.shortcode = auth.shortcode;
            parseResult.id = uri.substring(pattern.length);
            parseResult.initial = auth.initial;
            parseResult.color = auth.color;
            cont = false;
            return cont;
          }
        });
        return cont;
      });

      return parseResult;
    },

    /** We'll do this pre-processing step on the server later! **/
    getHierarchyPath : function(hierarchy) {
      var path = hierarchy.paths,
          last = path[path.length - 1],
          tuples = last.split('\u0007\u0007');

      return tuples.map(function(str) {
        var tuple = str.split('\u0007');
        return { 'id': tuple[0], 'title': tuple[1] };
      });
    },

    /** Shorthand to make handling of item types a bit easier **/
    getItemType : function(item) {
      var t = item.item_type;

      if (t.indexOf('OBJECT') > -1) {
        return 'OBJECT';
      } else if (t.indexOf('PLACE') > -1) {
        return 'PLACE';
      } else if (t.indexOf('PERSON') > -1) {
        return 'PERSON';
      } else if (t.indexOf('PERIOD') > -1) {
        return 'PERIOD';
      } else if (t.indexOf('DATASET') > -1) {
        return 'DATASET';
      }
    },

    getURIs : mapConflated('uri'),

    getDescriptions : mapConflated('descriptions'),

    getNames : mapConflated('names')

  };

});
