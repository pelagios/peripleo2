define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/state/search',
  'ui/state/urlBar'
], function(HasEvents, ItemUtils, Search, URLBar) {

  var State = function() {

    var clearSearch = function(refreshUI) {

        },

        loadNextPage = function() {

        },

        updateQuery = function(query) {

        },

        updateFilters = function(diff) {

        },

        updateTimerange = function(range) {

        },

        openFilterPane = function() {

        },

        closeFilterPane = function() {

        },

        setLayerChanged = function(name) {

        },

        setSelection = function(item) {

        };

    this.clearSearch = clearSearch;
    this.loadNextPage = loadNextPage;
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
