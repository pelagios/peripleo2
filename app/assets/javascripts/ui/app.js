require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/controls/results/resultList',
  'ui/controls/search/searchPanel',
  'ui/controls/selection/selectionPanel',
  'ui/map/map',
  'ui/navigation/navigation',
  'ui/state/state'
], function(ResultList, SearchPanel, SelectionPanel, Map, Navigation, State) {

  jQuery(document).ready(function() {

    var body = jQuery(document.body),

        mapDiv = jQuery('<div id="map"></div>').appendTo(body),

        controlsDiv = jQuery('<div id="search-controls"></div>').appendTo(body),

        map = new Map(mapDiv[0]),

        searchPanel = new SearchPanel(controlsDiv),

        selectionPanel = new SelectionPanel(controlsDiv),

        resultList = new ResultList(controlsDiv),

        state = new State(),

        navigation = new Navigation(map, searchPanel, selectionPanel, resultList, state);

    map.on('filterByViewport', navigation.onFilterByViewport);
    map.on('selectPlace', navigation.onSelectMapMarker);
    map.on('move', navigation.onMapMove);
    map.on('changeBasemap', state.setBasemap);

    searchPanel.on('open', navigation.onOpenFilterPane);
    searchPanel.on('close', navigation.onCloseFilterPane);
    searchPanel.on('queryChange', navigation.onQueryPhraseChanged);
    searchPanel.on('selectSuggestOption', navigation.onSelectIdentifier);
    searchPanel.on('setFilter', navigation.onSetFilter);
    searchPanel.on('removeAllFilters', navigation.onRemoveAllFilters);
    searchPanel.on('timerangeChange', navigation.onTimerangeChange);

    selectionPanel.on('select', navigation.onSelectIdentifier);
    selectionPanel.on('setFilter', navigation.onSetFilter);
    selectionPanel.on('localSearch', navigation.onLocalSearch);
    selectionPanel.on('linkedDataView', navigation.onLinkedDataView);

    resultList.on('select', navigation.onSelectItem);
    resultList.on('nextPage', navigation.onNextPage);

    state.on('stateChange', navigation.onStateChange);

    state.init();
  });

});
