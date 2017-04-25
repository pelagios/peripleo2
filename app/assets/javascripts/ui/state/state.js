define([
  'ui/common/hasEvents',
  'ui/state/search',
  'ui/state/history'
], function(HasEvents, Search, History) {

  var State = function() {

    var self = this,

        // TODO set initial state from URL bar
        // TODO trigger upstream event in case there are non-default settings in URL bar
        uiState = {
          filterPaneOpen: false
        },

        search = new Search(), // TODO set initial search state from URL bar

        history = new History(),

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

        setQuery = function(query) {
          search.setQuery(query);
          pushState();
        },

        updateFilters = function(diff) {
          search.updateFilters(diff);
          pushState();
        },

        setTimerange = function(range) {
          search.setTimerange(range);
          pushState();
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

    search.on('response', this.forwardEvent('searchResponse'));
    history.on('changeState', setState);

    pushState(); // Push initial state

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
