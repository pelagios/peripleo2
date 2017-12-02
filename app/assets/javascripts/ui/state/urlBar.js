define([], function() {

  /** A utility to convert between state objects and the URL hash **/
  return {

    parseHash : function() {
      var hash = window.location.hash,
          keysVals = (hash.indexOf('#') === 0) ? hash.substring(1).split('&') : false,
          segments = {}, filters = {},

          decodeIfDefined = function(arg) {
            return (arg) ? decodeURIComponent(arg) : undefined;
          },

          addFilterIfDefined = function(key, decode_arg) {
            var value = segments[key], decoded;
            if (value) {
              decoded = (decode_arg) ? decodeURIComponent(value) : value;
              filters[key] = decoded.split(',');
            }
          };

      if (keysVals) {
        keysVals.forEach(function(keyVal) {
          var asArray = keyVal.split('='),
              key = asArray[0],
              value = asArray[1];

          segments[key] = value;
        });

        addFilterIfDefined('referencing', true);
        addFilterIfDefined('types', false);
        addFilterIfDefined('datasets', true);

        return {
          search: {
            query : segments.q,
            filters : filters,
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
          selection: decodeIfDefined(segments.selected),
          ui: {
            basemap        : segments.basemap,
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
      setIfDefined(state.search.filters.referencing, 'referencing');
      setIfDefined(state.search.filters.types, 'types');
      setIfDefined(state.search.filters.datasets, 'datasets');
      setIfDefined(state.search.timerange.from, 'from');
      setIfDefined(state.search.timerange.to, 'to');
      setIfDefined(state.selection, 'selected');
      setIfDefined(state.ui.filterPaneOpen, 'filters');
      setIfDefined(state.ui.basemap, 'basemap');

      return jQuery.map(urlParams, function(val, key) {
        return key + '=' + val;
      }).join('&');
    }

  };

});
