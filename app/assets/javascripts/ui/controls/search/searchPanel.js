define([
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/filterPane',
  'ui/controls/search/searchbox/searchBox'
], function(HasEvents, FilterPane, SearchBox) {

  var SearchPanel = function(parentEl) {

    var self = this,

        element = jQuery('<div id="searchpanel"></div>').appendTo(parentEl),

        /** The query input field and associated autocomplete drop-down **/
        searchBox = new SearchBox(element),

        /** The collapsible filterpanel and footer **/
        filterPane = new FilterPane(element),

        /** (De)activates the loading spinner **/
        setLoading = function(loading) {
          searchBox.setLoading(loading);
        },

        /** Clears the footer result count **/
        clearFooter = function() {
          filterPane.clearFooter();
        },

        /** (De)activates the 'filter by viewport' indicator icon **/
        setFilterByViewport = function(filter) {
          filterPane.setFilterByViewport(filter);
        },

        /** Updates the filter indicators in the search bar **/
        updateFilterIndicators = function(filterSetting) {
          searchBox.updateIndicators(filterSetting);
        },

        /** Removes a group or a specific filter indicator from the search bar **/
        removeFilterIndicators = function(filterType, opt_identifier) {
          searchBox.removeIndicators(filterType, opt_identifier);
        },

        /** Updates all components with a new search response **/
        setSearchResponse = function(searchResponse) {
          filterPane.setSearchResponse(searchResponse);
        },

        onClearAll = function() {
          clearFooter();
          self.fireEvent('clearAll');
        },

        onTimerangeChange = function(interval) {
          if (interval.from || interval.to)
            searchBox.showTimefilterIndicator(interval);
          else
            searchBox.hideTimefilterIndicator();

          self.fireEvent('timerangeChange', interval);
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
          console.log(state);
          searchBox.setSearch(state.search);
          filterPane.setOpen(state.ui.filterPaneOpen);
        };

    // Forward events from child components up the hierarchy
    searchBox.on('change', this.forwardEvent('queryChange'));
    searchBox.on('selectSuggestOption', this.forwardEvent('selectSuggestOption'));
    searchBox.on('clearAll', onClearAll);

    filterPane.on('open', this.forwardEvent('open'));
    filterPane.on('close', this.forwardEvent('close'));
    filterPane.on('setFilter', this.forwardEvent('setFilter'));
    filterPane.on('timerangeChange', onTimerangeChange);

    this.setLoading = setLoading;
    this.clearFooter = clearFooter;
    this.setFilterByViewport = setFilterByViewport;
    this.updateFilterIndicators = updateFilterIndicators;
    this.removeFilterIndicators = removeFilterIndicators;
    this.setSearchResponse = setSearchResponse;
    this.setState = setState;

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
