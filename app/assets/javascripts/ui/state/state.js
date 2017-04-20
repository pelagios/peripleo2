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
          // TODO clear search args from URL bar
          // TODO if refreshUI == true, record a step in the history
        },

        updateQuery = function(query) {
          search.updateQuery(query);
          // TODO update URL bar
          // TODO record history step
        },

        updateFilters = function(diff) {
          search.updateFilters(diff);
          // TODO update URL bar
          // TODO record history step
        },

        updateTimerange = function(range) {
          search.updateTimerange(range);
          // TODO update URL bar
          // TODO record history step
        },

        openFilterPane = function() {
          search.setAggregationsEnabled(true);
          // TODO update URL bar
          // TODO should we treat this as a history step as well? (Probably...)
        },

        closeFilterPane = function() {
          search.setAggregationsEnabled(false);
          // TODO update URL bar
          // TODO should we treat this as a history step as well? (Probably...)
        },

        setLayerChanged = function(name) {
          // TODO update URL bar
          // TODO should we treat this as a history step as well? (Probably...)
        },

        setSelection = function(item) {
          // TODO update URL bar
          // TODO record history step
        };

    search.on('update', this.forwardEvent('update'));    

    this.clearSearch = clearSearch;
    this.loadNextPage = search.loadNextPage;
    this.updateQuery = updateQuery;
    this.updateFilters = updateFilters;
    this.updateTimerange = updateTimerange;
    this.openFilterPane = openFilterPane;
    this.closeFilterPane = closeFilterPane;
    this.setLayerChanged = setLayerChanged;
    this.setSelection = setSelection;

    HasEvents.apply(this);
  };
  State.prototype = Object.create(HasEvents.prototype);

  return State;

});
