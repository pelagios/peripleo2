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

        forwardEvent = function(event) {
          return function(obj) {
            self.fireEvent(event, obj);
          };
        },

        forwardFilterChange = function() {
          // TODO
        },

        update = function(searchResponse) {
          filterPane.update(searchResponse);
        };

    searchBox.on('change', forwardEvent('queryChange'));

    filterPane.on('open', forwardEvent('open'));
    filterPane.on('close', forwardEvent('close'));

    this.update = update;

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
