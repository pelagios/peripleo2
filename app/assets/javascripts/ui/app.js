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
  'ui/navigation/navigation',
  'ui/state/state',
  'ui/api',
], function(ItemUtils, ResultList, SearchPanel, SelectionPanel, Map, Navigation, State, API) {

      /** Shorthand for a 'transient query' state update **/
  var NOOP = { pushState: false, makeRequest: false },

      /**
       * A composition helper that puts a function in sequences with a jQuery deferred function.
       * I.e. function b is called after function a is .done(), with the results of a as input.
       */
      seq = function(a, b) { return function(arg) { a(arg).done(b); }; };

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

    // TODO activate load spinner
    // searchPanel.on('timerangeChange', seq(state.setTimerange, onSearchResponse));

    selectionPanel.on('select', navigation.onSelectIdentifier);
    selectionPanel.on('setFilter', navigation.onSetFilter);
    selectionPanel.on('localSearch', navigation.onLocalSearch);

    resultList.on('select', navigation.onSelectItem);
    resultList.on('nextPage', seq(state.loadNextPage, resultList.appendNextPage));

    state.on('stateChange', navigation.onStateChange);

    state.init();
  });

});
