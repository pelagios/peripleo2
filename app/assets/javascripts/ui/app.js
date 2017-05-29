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
  var seq = function(a, b) { return function(arg) { a(arg).done(b); }; };

  jQuery(document).ready(function() {
    var body = jQuery(document.body),

        mapDiv = jQuery('<div id="map"></div>').appendTo(body),

        controlsDiv = jQuery('<div id="search-controls"></div>').appendTo(body),

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
        onStateChange = function(e) {
          var state = e.state, // The state, as manipulated by the user (or set by the URL)
              request = e.request; // Promise of the search request triggered by the state change

          searchPanel.setState(state);
                  map.setState(state);

          if (state.selected)
            onSelectIdentifier(state.selected);

          request.done(onSearchResponse);
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

                var uri = ItemUtils.getURIs(item)[0],

                    fetchResultCountForReference = function(uri) {
                      var filter = { places : [ uri ] };

                      return state.updateFilters(filter, { pushState: false })
                        .then(function(results) {
                          state.updateFilters({ places: false }, { pushState: false, makeRequest: false });
                          return { 'identifier' : uri, 'resultCount' : results.total };
                        });
                    },

                    fetchReferences = API.getReferences(uri).then(function(references) {
                      // Run filtered searches for the first two references of each type,
                      // so we can display info in the UI
                      var places = (references.PLACE) ? references.PLACE.slice(0, 3) : false,

                          fPlaceCounts; // TODO support persona and period references

                      if (places) {
                        fPlaceCounts = places.map(function(place) {
                          return fetchResultCountForReference(place.identifiers[0]);
                        });

                        return jQuery.when.apply(jQuery, fPlaceCounts).then(function() {
                          return { references: references, resultCounts: arguments };
                        });
                      } else {
                        return jQuery.Deferred().resolve(this).then(function() {
                          return { references: references, resultCounts: [] };
                        });
                      }
                    });

                fetchReferences.done(function(result) {
                  var references = result.references,
                      resultCounts = result.resultCounts;

                  state.setSelectedItem(item);
                  selectionPanel.show(item, references, resultCounts);
                  resultList.setSelectedItem(item);

                  // Note: selection may have happend through the map, so technically no
                  // need for this - but the map is designed to handle this situation
                  map.setSelectedItem(item, references.PLACE);

                  // TODO currentSelection = { item: item, references: references }
                  currentSelection = item;
                });
              },

              // Selecting a place should select the first ITEM at this place, rather then
              // the place itself. To do this, we need to:
              // - issue a search request, filtered by the place
              // - select the first item in the search response
              // But we DON'T want:
              // - the request to show up in the history
              // - the rest of the UI to change
              // - the place filter to remain active after the request
              selectPlace = function(place) {
                var uri = ItemUtils.getURIs(place)[0],
                    filter = { places : [ uri ] };

                return state.updateFilters(filter, { pushState: false })
                  .then(function(results) {
                    state.updateFilters({ places: false }, { pushState: false, makeRequest: false });
                    return results.items[0];
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
                selectPlace(item).then(selectItem);
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
          API.getItem(identifier)
            .done(onSelectItem)
            .fail(function(error) {
              // TODO shouldn't happen unless connection or backend is down
              // TODO show error popup
            });
        },

        onFilterByReference = function(reference) {
          // TODO support filter by person | period
          state.updateFilters({ places : [ reference.identifiers[0] ] }).done(function(results) {
            resultList.setFilteredResponse(results, reference);
          });
        },

        onExitFilteredSearch = function() {
          state.updateFilters({ places : false }).done(function(results) {
            resultList.setSearchResponse(results);
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
    selectionPanel.on('filterBy', onFilterByReference);

    resultList.on('select', onSelectItem);
    resultList.on('nextPage', seq(state.loadNextPage, resultList.appendPage));
    resultList.on('exitFilteredSearch', onExitFilteredSearch);

    state.on('stateChange', onStateChange);
    state.init();
  });

});
