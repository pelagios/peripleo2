define([
  'ui/common/hasEvents',
  'ui/common/itemUtils'
], function(HasEvents, ItemUtils) {

  var History = function() {

    var buildURLHash = function(searchState, uiState) {
          var urlParams = {},

              setIfDefined = function(arg, name) {
                if (arg) urlParams[name] = arg;
              };

          setIfDefined(searchState.query, 'q');
          // TODO filters
          setIfDefined(searchState.timerange.from, 'from');
          setIfDefined(searchState.timerange.to, 'to');
          // TODO selection
          setIfDefined(uiState.filterPaneOpen, 'filters');

          return jQuery.map(urlParams, function(val, key) {
            return key + '=' + val;
          }).join('&');
        },

        pushState = function(searchState, uiState) {
          var state = { search: searchState, ui: uiState },
              hash = buildURLHash(searchState, uiState);

          window.history.pushState(state, null, '#' + hash);
        },

        onStateBack = function(e) {
          console.log(e.originalEvent.state);
        };

    jQuery(window).bind('popstate', onStateBack);

    this.pushState = pushState;

    HasEvents.apply(this);
  };
  History.prototype = Object.create(HasEvents.prototype);

  return History;

});
