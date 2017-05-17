define([
  'ui/common/hasEvents',
  'ui/state/search',
  'ui/state/history',
  'ui/state/urlBar'
], function(HasEvents, Search, History, URLBar) {

  var State = function() {

    var self = this,

        search = new Search(), // TODO set initial search state from URL bar

        history = new History(),

        currentSelection = false,

        uiState = {
          filterPaneOpen: false
        },

        init = function() {
          var initialState = URLBar.parseHash();
          setState(initialState);
          pushState();
        },

        pushState = function() {
          history.pushState(search.getCurrentArgs(), currentSelection, jQuery.extend({}, uiState));
        },

        setState = function(state) {
          if (state) {
            uiState = state.ui;
            self.fireEvent('stateChange', {
              state   : state,
              request : search.set(state.search)
            });
          }
        },

        clearSearch = function(options) {
          var pState = (options) ? options.pushState !== false : true, // default true
              makeRequest = (options) ? options.makeRequest !== false : true,
              promise = search.clear(makeRequest);

          if (pState) pushState();
          return promise;
        },

        setQueryPhrase = function(query) {
          var promise = search.setQuery(query);
          pushState();
          return promise;
        },

        // TODO remove redundancy with clearSearch
        updateFilters = function(diff, options) {
          var pState = (options) ? options.pushState !== false : true, // default true
              makeRequest = (options) ? options.makeRequest !== false : true,
              promise = search.updateFilters(diff, makeRequest);

          if (pState) pushState();
          return promise;
        },

        setTimerange = function(range) {
          var promise = search.setTimerange(range);
          pushState();
          return promise;
        },

        setFilterPaneOpen = function(open) {
          uiState.filterPaneOpen = open;
          var promise = search.setAggregationsEnabled(open);
          pushState();
          return promise;
        },

        setLayerChanged = function(name) {
          // TODO update URL bar
          // TODO should we treat this as a history step as well? (Probably...)
        },

        setSelectedItem = function(item) {
          currentSelection = item.is_conflation_of[0].uri;
          pushState();
        };

    history.on('changeState', setState);

    this.init = init;
    this.clearSearch = clearSearch;
    this.loadNextPage = search.loadNextPage;
    this.setQueryPhrase = setQueryPhrase;
    this.updateFilters = updateFilters;
    this.setTimerange = setTimerange;
    this.setFilterPaneOpen = setFilterPaneOpen;
    this.setLayerChanged = setLayerChanged;
    this.setSelectedItem = setSelectedItem;

    HasEvents.apply(this);
  };
  State.prototype = Object.create(HasEvents.prototype);

  return State;

});
