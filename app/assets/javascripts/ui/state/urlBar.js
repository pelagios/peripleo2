define([], function() {

  /**
   * A utility to convert between state objects and the URL hash
   */
  return {

    parseHash : function() {
      var hash = window.location.hash,
          keysVals = (hash.indexOf('#') === 0) ? hash.substring(1).split('&') : false,
          segments = {},

          decodeIfDefined = function(arg) {
            return (arg) ? decodeURIComponent(arg) : undefined;
          };

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
            filters : {}, // TODO
            timerange : {
              from : segments.from,
              to   : segments.to
            },
            settings : {
              timeHistogram    : segments.filters,
              termAggregations : segments.filters,
              topReferenced    : true
            }
          },
          selected: decodeIfDefined(segments.selected),
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
            if (arg) urlParams[name] = encodeURIComponent(arg);
          };

      setIfDefined(state.search.query, 'q');
      // TODO filters
      setIfDefined(state.search.timerange.from, 'from');
      setIfDefined(state.search.timerange.to, 'to');
      setIfDefined(state.selection, 'selected');
      setIfDefined(state.ui.filterPaneOpen, 'filters');

      return jQuery.map(urlParams, function(val, key) {
        return key + '=' + val;
      }).join('&');
    }

  };

});
