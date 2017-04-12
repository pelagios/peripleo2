require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/controls/results/resultList',
  'ui/controls/search/searchPanel',
  'ui/controls/selection/selectionPanel',
  'ui/map/map',
  'ui/api',
  'ui/search'
], function(ResultList, SearchPanel, SelectionPanel, Map, API, Search) {

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
          API.getItem(option.identifier).done(function(item) {
            console.log(item);
            selectionPanel.show(item);
            // TODO show on map
          }).fail(function(error) {
            // TODO shouldn't happen unless connection or backend is down
            // TODO show error popup
          });
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
