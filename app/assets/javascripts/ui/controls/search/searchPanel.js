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

        update = function(searchResponse) {
          filterPane.update(searchResponse);
        };

    searchBox.on('change', this.forwardEvent('queryChange'));

    filterPane.on('open', this.forwardEvent('open'));
    filterPane.on('close', this.forwardEvent('close'));
    filterPane.on('timerangeChange', this.forwardEvent('timerangeChange'));

    this.update = update;

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
