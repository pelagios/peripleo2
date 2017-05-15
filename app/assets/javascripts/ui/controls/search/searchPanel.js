define([
  'ui/common/hasEvents',
  'ui/controls/search/filterPane',
  'ui/controls/search/searchBox'
], function(HasEvents, FilterPane, SearchBox) {

  var SearchPanel = function(parentEl) {

    var self = this,

        element = jQuery('<div id="searchpanel"></div>').appendTo(parentEl),

        searchBox = new SearchBox(element),

        filterPane = new FilterPane(element),

        setSearchResponse = function(searchResponse) {
          filterPane.setResponse(searchResponse);
        },

        setState = function(state) {
          searchBox.setQuery(state.search.query);
          filterPane.setOpen(state.ui.filterPaneOpen);
        };

    searchBox.on('change', this.forwardEvent('queryChange'));
    searchBox.on('selectSuggestOption', this.forwardEvent('selectSuggestOption'));

    filterPane.on('open', this.forwardEvent('open'));
    filterPane.on('close', this.forwardEvent('close'));
    filterPane.on('timerangeChange', this.forwardEvent('timerangeChange'));

    this.setSearchResponse = setSearchResponse;
    this.setState = setState;

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
