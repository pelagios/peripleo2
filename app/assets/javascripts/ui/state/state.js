define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/state/search',
  'ui/state/urlBar'
], function(HasEvents, ItemUtils, Search, URLBar) {

  var State = function() {

    var search = new Search(),

        urlBar = new URLBar(),

        clearSearch = function(refreshUI) {
          search.clear(refreshUI);

          urlBar.setQuery();
          urlBar.clearFilters();
          urlBar.setTimerange();

          // TODO if refreshUI == true, record a step in the history
        },

        setQuery = function(query) {
          search.setQuery(query);
          urlBar.setQuery(query);
          // TODO record history step
        },

        updateFilters = function(diff) {
          search.updateFilters(diff);
          urlBar.updateFilters(diff);
          // TODO record history step
        },

        setTimerange = function(range) {
          search.setTimerange(range);
          urlBar.setTimerange(range);
          // TODO record history step
        },

        openFilterPane = function() {
          search.setAggregationsEnabled(true);
          urlBar.setFilterpaneOpen(true);
          // TODO should we treat this as a history step as well? (Probably...)
        },

        closeFilterPane = function() {
          search.setAggregationsEnabled(false);
          urlBar.setFilterpaneOpen(false);
          // TODO should we treat this as a history step as well? (Probably...)
        },

        setLayerChanged = function(name) {
          // TODO update URL bar
          // TODO should we treat this as a history step as well? (Probably...)
        },

        setSelection = function(item) {
          urlBar.setSelection(item);
          // TODO record history step
        };

    search.on('update', this.forwardEvent('update'));

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
