define([
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/filterPane',
  'ui/controls/search/searchbox/searchBox',
  'ui/controls/search/filterCrumbs'
], function(HasEvents, FilterPane, SearchBox, FilterCrumbs) {

  var SearchPanel = function(parentEl) {

    var self = this,

        element = jQuery('<div id="searchpanel"></div>').appendTo(parentEl),

        searchBox = new SearchBox(element),

        filterCrumbs = new FilterCrumbs(element),

        filterPane = new FilterPane(element),

        /** Activates the loading spinner **/
        setLoading = function(loading) {
          searchBox.setLoading(loading);
        },

        setFilterByViewport = function(filter) {
          filterPane.setFilterByViewport(filter);
        },

        setSearchResponse = function(searchResponse) {
          // TODO if the search includes any filter - set X icon
          searchBox.setLoading(false);
          filterPane.setResponse(searchResponse);
        },

        setState = function(state) {
          searchBox.setQuery(state.search.query);
          filterPane.setOpen(state.ui.filterPaneOpen);
        },

        updateFilterCrumbs = function(diff) {
          filterCrumbs.update(diff);
        };

    searchBox.on('change', this.forwardEvent('queryChange'));
    searchBox.on('selectSuggestOption', this.forwardEvent('selectSuggestOption'));

    filterCrumbs.on('removeAll', this.forwardEvent('removeAllFilters'));

    filterPane.on('open', this.forwardEvent('open'));
    filterPane.on('close', this.forwardEvent('close'));
    filterPane.on('setFilter', this.forwardEvent('setFilter'));
    filterPane.on('timerangeChange', this.forwardEvent('timerangeChange'));

    this.setSearchResponse = setSearchResponse;
    this.setState = setState;
    this.setLoading = setLoading;
    this.setFilterByViewport = setFilterByViewport;
    this.updateFilterCrumbs = updateFilterCrumbs;

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
