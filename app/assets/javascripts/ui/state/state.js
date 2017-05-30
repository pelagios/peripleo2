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

        // Common functionality for changing properties of the search
        changeSearch = function(change, options) {
          var pState = (options) ? options.pushState !== false : true, // default true
              makeRequest = (options) ? options.makeRequest !== false : true,
              promise = change(makeRequest);

          if (pState) pushState();
          return promise;
        },

        clearSearch = function(options) {
          return changeSearch(search.clear, options);
        },

        setQueryPhrase = function(query, options) {
          var changeFn = function(makeRequest)  {
                return search.setQuery(query, makeRequest);
              };
          return changeSearch(changeFn, options);
        },

        updateFilters = function(diff, options) {
          var changeFn = function(makeRequest) {
                return search.updateFilters(diff, makeRequest);
              };
          return changeSearch(changeFn, options);
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
    this.getQueryPhrase = search.getQuery;
    this.updateFilters = updateFilters;
    this.setTimerange = setTimerange;
    this.setFilterPaneOpen = setFilterPaneOpen;
    this.setLayerChanged = setLayerChanged;
    this.setSelectedItem = setSelectedItem;
    this.setViewport = search.setViewport;

    HasEvents.apply(this);
  };
  State.prototype = Object.create(HasEvents.prototype);

  return State;

});
