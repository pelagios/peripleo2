define([], function() {

  /**
   * A utility to convert between state objects and the URL hash
   */
  return {

    parseHash : function() {
      // TODO return state object from current hash
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
