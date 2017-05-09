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

        // TODO set initial state from URL bar
        // TODO trigger upstream event in case there are non-default settings in URL bar
        uiState = {
          filterPaneOpen: false
        },

        init = function() {
          var initialState = URLBar.parseHash();
          setState(initialState);
          pushState();
        },

        pushState = function() {
          history.pushState(search.getCurrentArgs(), jQuery.extend({}, uiState));
        },

        setState = function(state) {
          if (state) {
            uiState = state.ui;
            search.set(state.search);
            self.fireEvent('stateUpdate', state);
          }
        },

        clearSearch = function(refreshUI) {
          search.clearSearch(refreshUI);
          if (refreshUI) pushState();
        },

        setQuery = function(query, opt_onetime_settings) {
          var promise = search.setQuery(query, opt_onetime_settings);
          pushState();
          return promise;
        },

        updateFilters = function(diff, opt_onetime_settings) {
          var promise = search.updateFilters(diff, opt_onetime_settings);
          pushState();
          return promise;
        },

        setTimerange = function(range, opt_onetime_settings) {
          var promise = search.setTimerange(range, opt_onetime_settings);
          pushState();
          return promise;
        },

        openFilterPane = function() {
          uiState.filterPaneOpen = true;
          search.setAggregationsEnabled(true);
          pushState();
        },

        closeFilterPane = function() {
          uiState.filterPaneOpen = false;
          search.setAggregationsEnabled(false);
          pushState();
        },

        setLayerChanged = function(name) {
          // TODO update URL bar
          // TODO should we treat this as a history step as well? (Probably...)
        },

        setSelection = function(item) {
          // urlBar.setSelection(item);
          // TODO record history step
        };

    search.on('searchResponse', this.forwardEvent('searchResponse'));
    search.on('nextPageResponse', this.forwardEvent('nextPageResponse'));

    history.on('changeState', setState);

    this.init = init;
    this.clearSearch = clearSearch;
    this.loadNextPage = search.loadNextPage;
    this.setQuery = setQuery;
    this.updateFilters = updateFilters;
    this.setTimerange = setTimerange;
    this.openFilterPane = openFilterPane;
    this.closeFilterPane = closeFilterPane;
    this.setLayerChanged = setLayerChanged;
    this.setSelection = setSelection;

    HasEvents.apply(this);
  };
  State.prototype = Object.create(HasEvents.prototype);

  return State;

});
