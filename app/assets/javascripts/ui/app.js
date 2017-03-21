require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/controls/results/resultList',
  'ui/controls/search/searchPanel',
  'ui/controls/selection/selectionPanel',
  'ui/map/map',
  'ui/api'
], function(ResultList, SearchPanel, SelectionPanel, Map, API) {

  jQuery(document).ready(function() {
    var body = jQuery(document.body),

        mapDiv = jQuery('<div id="map"></div>').appendTo(body),

        controlsDiv = jQuery('<div id="controls"></div>').appendTo(body),

        map = new Map(mapDiv[0]),

        searchPanel = new SearchPanel(controlsDiv),

        selectionPanel = new SelectionPanel(controlsDiv),

        resultList = new ResultList(controlsDiv),

        api = new API(),

        onUpdate = function(response) {
          searchPanel.update(response);
           resultList.update(response);
                  map.update(response);
        };

    searchPanel.on('open', api.enableAggregations);
    searchPanel.on('close', api.disableAggregations);
    searchPanel.on('queryChange', api.updateQuery);
    searchPanel.on('timerangeChange', api.updateTimerange);

    resultList.on('select', selectionPanel.show);
    resultList.on('nextPage', api.loadNextPage);

    api.on('update', onUpdate);
  });

});
