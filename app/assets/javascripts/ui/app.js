require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/controls/results/resultList',
  'ui/controls/search/searchPanel',
  'ui/controls/selection/selectionPanel',
  'ui/map/map',
  'ui/search'
], function(ResultList, SearchPanel, SelectionPanel, Map, Search) {

  jQuery(document).ready(function() {
    var body = jQuery(document.body),

        mapDiv = jQuery('<div id="map"></div>').appendTo(body),

        controlsDiv = jQuery('<div id="controls"></div>').appendTo(body),

        map = new Map(mapDiv[0]),

        searchPanel = new SearchPanel(controlsDiv),

        selectionPanel = new SelectionPanel(controlsDiv),

        resultList = new ResultList(controlsDiv),

        search = new Search(),

        onUpdate = function(response) {
          searchPanel.update(response);
           resultList.update(response);
                  map.update(response);
        },

        onSelectSuggestOption = function(option) {
          // TODO fetch via API
          // TODO selectionPanel.show
          console.log('direct select', option);
        };

    searchPanel.on('open', search.enableAggregations);
    searchPanel.on('close', search.disableAggregations);
    searchPanel.on('queryChange', search.updateQuery);
    searchPanel.on('timerangeChange', search.updateTimerange);
    searchPanel.on('selectSuggestOption', onSelectSuggestOption);

    resultList.on('select', selectionPanel.show);
    resultList.on('nextPage', search.loadNextPage);

    search.on('update', onUpdate);
  });

});
