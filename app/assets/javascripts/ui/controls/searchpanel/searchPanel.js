define([
  'ui/common/hasEvents',
  'ui/controls/searchpanel/filterPane',
  'ui/controls/searchpanel/searchBox'
], function(HasEvents, FilterPane, SearchBox) {

  var SearchPanel = function(parentEl) {

    var self = this,

        element = jQuery('<div id="searchpanel"></div>').appendTo(parentEl),

        searchBox = new SearchBox(element),

        filterPane = new FilterPane(element),

        forwardQueryChange = function(query) {
          self.fireEvent('queryChange', query);
        },

        forwardFilterChange = function() {
          // TODO
        },

        update = function(searchResponse) {
          filterPane.update(searchResponse);
        };

    searchBox.on('change', forwardQueryChange);
    filterPane.on('change', forwardFilterChange);

    this.update = update;

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
