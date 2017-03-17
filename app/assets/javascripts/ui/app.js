require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/controls/resultlist/resultList',
  'ui/controls/searchpanel/searchPanel',
  'ui/map/map',
  'ui/api'
], function(ResultList, SearchPanel, Map, API) {

  jQuery(document).ready(function() {
    var body = jQuery(document.body),

        mapDiv = jQuery('<div id="map"></div>').appendTo(body),

        controlsDiv = jQuery('<div id="controls"></div>').appendTo(body),

        map = new Map(mapDiv[0]),

        searchPanel = new SearchPanel(controlsDiv),

        resultList = new ResultList(controlsDiv),

        api = new API(),

        onUpdate = function(response) {
          searchPanel.update(response);
           resultList.update(response);
                  map.update(response);
        };

    searchPanel.on('queryChange', api.updateQuery);
    searchPanel.on('open', api.enableAggregations);
    searchPanel.on('close', api.disableAggregations);

    resultList.on('nextPage', api.loadNextPage);

    api.on('update', onUpdate);
  });

});
