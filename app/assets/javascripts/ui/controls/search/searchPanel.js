define([
  'ui/common/hasEvents',
  'ui/controls/search/filtercrumbs/filterCrumbs',
  'ui/controls/search/filterpane/filterPane',
  'ui/controls/search/searchbox/searchBox'
], function(HasEvents, FilterCrumbs, FilterPane, SearchBox) {

  var SearchPanel = function(parentEl) {

    var self = this,

        element = jQuery('<div id="searchpanel"></div>').appendTo(parentEl),

        /** The query input field and associated autocomplete drop-down **/
        searchBox = new SearchBox(element),

        /** The 'filter crumbs' bar in between the search box and the panel footer **/
        filterCrumbs = new FilterCrumbs(element),

        /** The collapsible filterpanel and footer **/
        filterPane = new FilterPane(element),

        /** (De)activates the loading spinner **/
        setLoading = function(loading) {
          searchBox.setLoading(loading);
        },

        /** (De)activates the 'filter by viewport' indicator icon **/
        setFilterByViewport = function(filter) {
          filterPane.setFilterByViewport(filter);
        },

        /** Updates the filterCrumbs bar **/
        updateFilterCrumbs = function(filterSetting) {
          filterCrumbs.update(filterSetting);
        },

        removeFilterCrumbs = function(filterType, opt_identifier) {
          filterCrumbs.remove(filterType, opt_identifier);
        },

        /** Updates all components with a new search response **/
        setSearchResponse = function(searchResponse) {
          filterPane.setSearchResponse(searchResponse);
        },

        /**
         * Sets a new state on all components.
         *
         * This happends on page load (based on initial query URL args), or because
         * the user clicked the back button. The only relevant state parameters at
         * the moment are:
         * - the query phrase
         * - the open/closed state of the panel
         */
        setState = function(state) {
          searchBox.setQuery(state.search.query);
          filterPane.setOpen(state.ui.filterPaneOpen);
        };

    // Forward events from child components up the hierarchy
    searchBox.on('change', this.forwardEvent('queryChange'));
    searchBox.on('selectSuggestOption', this.forwardEvent('selectSuggestOption'));

    filterCrumbs.on('removeAll', this.forwardEvent('removeAllFilters'));

    filterPane.on('open', this.forwardEvent('open'));
    filterPane.on('close', this.forwardEvent('close'));
    filterPane.on('setFilter', this.forwardEvent('setFilter'));
    filterPane.on('timerangeChange', this.forwardEvent('timerangeChange'));

    this.setLoading = setLoading;
    this.setFilterByViewport = setFilterByViewport;
    this.updateFilterCrumbs = updateFilterCrumbs;
    this.removeFilterCrumbs = removeFilterCrumbs;
    this.setSearchResponse = setSearchResponse;
    this.setState = setState;

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
