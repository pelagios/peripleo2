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

        currentSelection = false,

        onSearchResponse = function(response) {
          searchPanel.setResponse(response);
           resultList.setResponse(response);
                  map.setResponse(response);
        },

        onStateUpdate = function(state) {
          searchPanel.setState(state);
                  map.setState(state);

          if (state.selected)
            onSelectIdentifier(state.selected);
        },

        deselect = function() {
          var deselectItem = function(item) {
                selectionPanel.hide();
                currentSelection = false;
              },

              deselectPlace = function(item) {
                state.updateFilters({ places : false });
                deselectItem(item);
              };

          if (currentSelection)
            switch(ItemUtils.getItemType(currentSelection)) {
              case 'PLACE':
                deselectItem(currentSelection);
                break;
              case 'OBJECT':
                deselectItem(currentSelection);
                break;
              case 'PERSON':
                deselectItem(currentSelection);
                break;
              case 'DATASET':
                selectDataset(currentSelection);
                break;
            }
        },

        /** An item was selected (e.g. via the result list) **/
        onSelectItem = function(item) {

              // Common select functionality
          var selectItem = function(item) {
                currentSelection = item;

                selectionPanel.show(item);
                resultList.setSelectedItem(item);
                state.setSelection(item);

                // TODO show on map

              },

              // Filter search (once) by this Place
              selectPlace = function(place) {
                var uri = ItemUtils.getURIs(place)[0],
                    filter = { places : [ uri ] },
                    onetimeSettings = { topPlaces: false };
                    
                state.updateFilters(filter, onetimeSettings)
                  .done(function(results) {
                    selectItem(results.items[0]);
                  });
              },

              // Apply a filter to show everything in this dataset
              selectDataset = function(dataset) {
                var uri = dataset.is_conflation_of[0].uri;
                selectItem(dataset);
                state.clearSearch(false);
                state.updateFilters({ 'datasets': uri });
              };

          if (item)
            switch(ItemUtils.getItemType(item)) {
              case 'PLACE':
                selectPlace(item);
                break;
              case 'OBJECT':
                selectItem(item);
                break;
              case 'PERSON':
                selectItem(item);
                break;
              case 'DATASET':
                selectDataset(item);
                break;
            }
          else
            deselect();
        },

        /** An identifier was selected (e.g. via suggestions) - fetch item **/
        onSelectIdentifier = function(identifier) {

          // Clear search results


          API.getItem(identifier)
            .done(onSelectItem)
            .fail(function(error) {
              // TODO shouldn't happen unless connection or backend is down
              // TODO show error popup
            });
        };

    // controlsDiv.resizable({ handles: 'e' });

    map.on('selectPlace', onSelectItem);

    searchPanel.on('open', state.openFilterPane);
    searchPanel.on('close', state.closeFilterPane);
    searchPanel.on('queryChange', state.setQuery);
    searchPanel.on('timerangeChange', state.setTimerange);
    searchPanel.on('selectSuggestOption', onSelectIdentifier);

    selectionPanel.on('select', onSelectIdentifier);

    resultList.on('select', onSelectItem);
    resultList.on('nextPage', state.loadNextPage);

    state.on('searchResponse', onSearchResponse);
    state.on('nextPageResponse', resultList.appendPage);

    state.on('stateUpdate', onStateUpdate);
    state.init();
  });

});
