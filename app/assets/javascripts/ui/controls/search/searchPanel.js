define([
  'ui/common/hasEvents',
  'ui/controls/search/filterpane/filterPane',
  'ui/controls/search/searchbox/searchBox'
], function(HasEvents, FilterPane, SearchBox) {

  var SearchPanel = function(parentEl) {

    var self = this,

        element = jQuery('<div id="searchpanel"></div>').appendTo(parentEl),

        searchBox = new SearchBox(element),

        filterPane = new FilterPane(element),

        /** Activates the loading spinner **/
        loading = function() {
          searchBox.setLoading(true);
        },

        setSearchResponse = function(searchResponse) {
          // TODO if the search includes any filter - set X icon
          searchBox.setLoading(false);
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
    this.loading = loading;

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
