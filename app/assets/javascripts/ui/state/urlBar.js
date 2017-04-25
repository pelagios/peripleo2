define([], function() {

  /**
   * A utility to convert between state objects and the URL hash
   */
  return {

    parseHash : function() {
      var hash = window.location.hash,
          keysVals = (hash.indexOf('#') === 0) ? hash.substring(1).split('&') : false,
          segments = {};

      if (keysVals) {
        keysVals.forEach(function(keyVal) {
          var asArray = keyVal.split('='),
              key = asArray[0],
              value = asArray[1];

          segments[key] = value;
        });

        return {
          search: {
            query : segments.q,
            timerange : {
              from : segments.from,
              to   : segments.to
            },
            settings : {
              timeHistogram    : segments.filters,
              termAggregations : false,
              topPlaces        : true
            }
          },
          ui: {
            filterPaneOpen : segments.filters
          }
        };
      } else {
        return undefined;
      }
    },

    toHash : function(state) {
      var urlParams = {},

          setIfDefined = function(arg, name) {
            if (arg) urlParams[name] = arg;
          };

      setIfDefined(state.search.query, 'q');
      // TODO filters
      setIfDefined(state.search.timerange.from, 'from');
      setIfDefined(state.search.timerange.to, 'to');
      // TODO selection
      setIfDefined(state.ui.filterPaneOpen, 'filters');

      return jQuery.map(urlParams, function(val, key) {
        return key + '=' + val;
      }).join('&');
    }

  };

});