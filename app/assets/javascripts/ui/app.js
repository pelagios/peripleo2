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

  /**
   * A composition helper that puts a function in sequences with a jQuery deferred function.
   * I.e. function b is called after function a is .done(), with the results of a as input.
   */
  var seq = function(a, b) {
    return function(arg) { a(arg).done(b); };
  };

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
          selectionPanel.hide();
             searchPanel.setSearchResponse(response);
              resultList.setSearchResponse(response);
                     map.setSearchResponse(response);
        },

        // Happens on 'Back' button, or on page load (when URL params are set)
        onStateChange = function(state) {
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
                deselectPlace(currentSelection);
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

        /**
         * An item was selected, either via:
         * - the result list
         * - a map marker
         * - through autosuggest->identifier->API fetch
         */
        onSelectItem = function(item) {

              // Common select functionality
          var selectItem = function(item) {
                var uri = ItemUtils.getURIs(item)[0];

                API.getReferences(uri).done(function(references) {
                  state.setSelectedItem(item);
                  selectionPanel.show(item, references);
                  resultList.setSelectedItem(item);

                  // Note: selection may have happend through the map, so technically no
                  // need for this - but the map is designed to handle this situation
                  map.setSelectedItem(item, references.PLACE);

                  // TODO currentSelection = { item: item, references: references }
                  currentSelection = item;
                });
              },

              // Selecting a place should select the first item at this place.
              // To do this, we need to:
              // - issue a search request, filtered by the place
              // - select the first item in the search response
              // We DON'T want:
              // - the rest of the UI state (and history) to update after the request
              // - the place filter to remain active after the request
              selectPlace = function(place) {
                var uri = ItemUtils.getURIs(place)[0],
                    filter = { places : [ uri ] };

                state.updateFilters(filter, { updateState: false })
                  .done(function(results) {
                    selectItem(results.items[0]);
                    state.updateFilters({ places: false }, { updateState: false });
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

          // TODO remove query from state (but without firing a new request!)
          // TODO state.setQueryPhrase(false, { updateState: false });

          API.getItem(identifier)
            .done(onSelectItem)
            .fail(function(error) {
              // TODO shouldn't happen unless connection or backend is down
              // TODO show error popup
            });
        },

        onOpenFilterPane = function() {
          state.setFilterPaneOpen(true).done(onSearchResponse);
        },

        onCloseFilterPane = function() {
          // TODO this will internally fire a new search request (whose response
          // TODO gets ignored) - that's not really needed!
          state.setFilterPaneOpen(false);
        };

    map.on('selectPlace', onSelectItem);

    searchPanel.on('open', onOpenFilterPane);
    searchPanel.on('close', onCloseFilterPane);
    searchPanel.on('queryChange', seq(state.setQueryPhrase, onSearchResponse));
    searchPanel.on('timerangeChange', seq(state.setTimerange, onSearchResponse));
    searchPanel.on('selectSuggestOption', onSelectIdentifier);

    selectionPanel.on('select', onSelectIdentifier);

    resultList.on('select', onSelectItem);
    resultList.on('nextPage', seq(state.loadNextPage, resultList.appendPage));

    state.on('stateChange', onStateChange);
    state.init();
  });

});
