define([
  'ui/common/hasEvents',
  'ui/state/search',
  'ui/state/history',
  'ui/state/urlBar'
], function(HasEvents, Search, History, URLBar) {

  var State = function() {

    var self = this,

        search = new Search(),

        history = new History(),

        currentSelection = false,

        uiState = {
          basemap          : false, // the current basemap
          viewportBounds   : false, // the current map viewport
          filterPaneOpen   : false, // open/closed state of the filter panel
          filterByViewport : false  // state of the 'filter by viewport' toggle button
        },

        /** Shorthand to push search, current selection and UI state into the page history **/
        pushState = function() {
          history.pushState(search.getArgs(), currentSelection, jQuery.extend({}, uiState));
        },

        /**
         * Sets (rather than updates) a new state, completely replacing the old one.
         *
         * This method is used only when:
         * - initializing a new UI, based on the URL bar
         * - a 'changeState' event comes in from the history (i.e. the user clicked the back button)
         */
        setState = function(state) {
          var isSearchDefined = function() {
                var s = state.search;
                return s.query ||
                  !jQuery.isEmptyObject(s.filters) ||
                  s.timerange.from || s.timerange.to ||
                  s.bbox;
              },

              // We only need to trigger a search if there are search args, or the filter pane
              // is open (in which case it's ok to have a 'match all' query)
              makeReq = state.ui.filterPaneOpen || isSearchDefined();

          uiState = state.ui;
          self.fireEvent('stateChange', {
            state   : state,
            request : search.setArgs(state.search, makeReq)
          });
        },

        init = function() {
          var initialState = URLBar.parseHash();
          if (initialState) {
            setState(initialState);
            pushState();
          }
        },

        getUIState = function() {
          return uiState;
        },

        /** Common functionality for changing properties of the search **/
        changeSearch = function(change, options) {
          var pState = (options) ? options.pushState !== false : true, // default true
              makeRequest = (options) ? options.makeRequest !== false : true,
              promise = change(makeRequest);

          if (pState) pushState();
          return promise;
        },

        /** Completely clears the search, removing all filters and clearing the query **/
        clearSearch = function(options) {
          return changeSearch(search.clear, options);
        },

        /** Clears the search filters - does not affect the time interval! **/
        clearFilters = function(options) {
          return changeSearch(search.clearFilters, options);
        },

        /** Changes the search query phrase, leaving all other settings unchanged **/
        setQueryPhrase = function(query, options) {
          var changeFn = function(makeRequest)  {
                return search.setQuery(query, makeRequest);
              };
          return changeSearch(changeFn, options);
        },

        /** Updates the search filters **/
        updateFilters = function(diff, options) {
          var changeFn = function(makeRequest) {
                return search.updateFilters(diff, makeRequest);
              };
          return changeSearch(changeFn, options);
        },

        /** Sets the search time range **/
        setTimerange = function(range) {
          var promise = search.setTimerange(range);
          pushState();
          return promise;
        },

        /** Opens the filter pane (triggering a new search by default) **/
        setFilterPaneOpen = function(open, options) {
          var changeFn = function(makeRequest) {
                return search.setAggregationsEnabled(open, makeRequest);
              };

          uiState.filterPaneOpen = open;
          return changeSearch(changeFn, options);
        },

        /** Updates the viewport state (and triggers a new search if necessary) **/
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

        /** Toggles viewport filtering **/
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

        /** Updates the base map state **/
        setBasemap = function(name) {
          uiState.basemap = name;
          pushState();
        },

        /** Updates the selected item state **/
        setSelectedItem = function(item) {
          if (item)
            currentSelection = item.is_conflation_of[0].uri;
          else
            currentSelection = false;
          pushState();
        };

    history.on('changeState', setState);

    this.init = init;
    this.getUIState = getUIState;
    this.clearSearch = clearSearch;
    this.clearFilters = clearFilters;
    this.setQueryPhrase = setQueryPhrase;
    this.getQueryPhrase = search.getQuery;
    this.updateFilters = updateFilters;
    this.setTimerange = setTimerange;
    this.setFilterPaneOpen = setFilterPaneOpen;
    this.setViewport = setViewport;
    this.setFilterByViewport = setFilterByViewport;
    this.setBasemap = setBasemap;
    this.setSelectedItem = setSelectedItem;
    this.loadNextPage = search.loadNextPage;

    HasEvents.apply(this);
  };
  State.prototype = Object.create(HasEvents.prototype);

  return State;

});
