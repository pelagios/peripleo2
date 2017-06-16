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
          // The current map viewport
          viewportBounds   : false,

          // Open/closed state of the filter panel
          filterPaneOpen   : false,

          // State of the 'filter by viewport' button
          filterByViewport : false
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

        getUIState = function() {
          return uiState;
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

        clearFilters = function(options) {
          return changeSearch(search.clearFilters, options);
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

        setFilterPaneOpen = function(open, options) {
          var changeFn = function(makeRequest) {
                return search.setAggregationsEnabled(open, makeRequest);
              };

          uiState.filterPaneOpen = open;
          return changeSearch(changeFn, options);
        },

        setTimerange = function(range) {
          var promise = search.setTimerange(range);
          pushState();
          return promise;
        },

        setViewport = function(bounds, options) {
          // Update UI state
          uiState.viewportBounds = bounds;

          // If we are currently filtering by viewport, we need to trigger a search, too
          if (uiState.filterByViewport) {
            var changeFn = function(makeRequest) {
                  return search.setViewport(bounds, makeRequest);
                };
            return changeSearch(changeFn, options);
          }
        },

        setFilterByViewport = function(filter) {
          uiState.filterByViewport = filter;

          var promise = (filter) ?
                // If filter == true, we set the bounds in the search state...
                search.setViewport(uiState.viewportBounds, true) :
                // ...otherwise we remove them
                search.setViewport(false, true);

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
    this.clearFilters = clearFilters;
    this.loadNextPage = search.loadNextPage;
    this.setQueryPhrase = setQueryPhrase;
    this.getQueryPhrase = search.getQuery;
    this.getUIState = getUIState;
    this.updateFilters = updateFilters;
    this.setTimerange = setTimerange;
    this.setFilterPaneOpen = setFilterPaneOpen;
    this.setFilterByViewport = setFilterByViewport;
    this.setLayerChanged = setLayerChanged;
    this.setSelectedItem = setSelectedItem;
    this.setViewport = setViewport;

    HasEvents.apply(this);
  };
  State.prototype = Object.create(HasEvents.prototype);

  return State;

});
