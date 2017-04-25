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

        onSearchResponse = function(response) {
          searchPanel.setResponse(response);
           resultList.setResponse(response);
                  map.setResponse(response);
        },

        onStateUpdate = function(state) {
          searchPanel.setState(state);
                  map.setState(state);
        },

        /** An item was selected (e.g. via the result list) **/
        onSelectItem = function(item) {
          // TODO can we get this to match with the callback in onSelectIdentifier?
          selectionPanel.show(item);
          state.setSelection(item);

          // TODO show on map
        },

        /** An identifier was selected (e.g. via suggestions) - fetch item **/
        onSelectIdentifier = function(identifier) {
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
    searchPanel.on('queryChange', state.setQuery);
    searchPanel.on('timerangeChange', state.setTimerange);
    searchPanel.on('selectSuggestOption', onSelectIdentifier);

    selectionPanel.on('select', onSelectIdentifier);

    resultList.on('select', onSelectItem);
    resultList.on('nextPage', state.loadNextPage);

    state.on('searchResponse', onSearchResponse);
    state.on('stateUpdate', onStateUpdate);
    
    state.init();
  });

});
