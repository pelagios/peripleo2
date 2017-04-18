require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/common/itemUtils',
  'ui/controls/results/resultList',
  'ui/controls/search/searchPanel',
  'ui/controls/selection/selectionPanel',
  'ui/map/map',
  'ui/api',
  'ui/search'
], function(ItemUtils, ResultList, SearchPanel, SelectionPanel, Map, API, Search) {

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

        onSelectByIdentifier = function(identifier) {

          var selectDataset = function(dataset) {
                // TODO clear the current search, and replace with a dataset filter
                console.log('foo');
              };

          API.getItem(identifier).done(function(item) {
            selectionPanel.show(item);
            // TODO show on map

            switch(ItemUtils.getItemType(item)) {
              case 'DATASET':
                selectDataset(item);
                break;
            }
          }).fail(function(error) {
            // TODO shouldn't happen unless connection or backend is down
            // TODO show error popup
          });
        };

    searchPanel.on('open', search.enableAggregations);
    searchPanel.on('close', search.disableAggregations);
    searchPanel.on('queryChange', search.updateQuery);
    searchPanel.on('timerangeChange', search.updateTimerange);
    searchPanel.on('selectSuggestOption', onSelectByIdentifier);

    selectionPanel.on('select', onSelectByIdentifier);

    resultList.on('select', selectionPanel.show);
    resultList.on('nextPage', search.loadNextPage);

    search.on('update', onUpdate);
  });

});
