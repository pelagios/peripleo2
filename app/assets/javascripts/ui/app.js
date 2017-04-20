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
  'ui/state/state',
  'ui/api',
], function(ItemUtils, ResultList, SearchPanel, SelectionPanel, Map, State, API) {

  jQuery(document).ready(function() {
    var body = jQuery(document.body),

        mapDiv = jQuery('<div id="map"></div>').appendTo(body),

        controlsDiv = jQuery('<div id="controls"></div>').appendTo(body),

        map = new Map(mapDiv[0]),

        searchPanel = new SearchPanel(controlsDiv),

        selectionPanel = new SelectionPanel(controlsDiv),

        resultList = new ResultList(controlsDiv),

        state = new State(),

        onUpdate = function(response) {
          searchPanel.update(response);
           resultList.update(response);
                  map.update(response);
        },

        onSelectByIdentifier = function(identifier) {

          var selectDataset = function(dataset) {
                var uri = dataset.is_conflation_of[0].uri;
                state.clearSearch(false);
                state.updateFilters({ 'datasets': uri });
              };

          API.getItem(identifier).done(function(item) {
            selectionPanel.show(item);
            state.setSelection(item);

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

    searchPanel.on('open', state.openFilterPane);
    searchPanel.on('close', state.closeFilterPane);
    searchPanel.on('queryChange', state.updateQuery);
    searchPanel.on('timerangeChange', state.updateTimerange);
    searchPanel.on('selectSuggestOption', onSelectByIdentifier);

    selectionPanel.on('select', onSelectByIdentifier);

    resultList.on('select', selectionPanel.show);
    resultList.on('nextPage', state.loadNextPage);

    state.on('update', onUpdate);
  });

});
