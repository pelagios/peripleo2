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

        // Keeps track of current selection, so we can deselect
        currentSelection = false,

        // Keeps track of search query when moving to local search
        previousGlobalSearch = false,

        onSearchResponse = function(response) {
             searchPanel.setSearchResponse(response);
              resultList.setSearchResponse(response);
                     map.setSearchResponse(response);
        },

        // Happens on 'Back' button, or on page load (when URL params are set)
        onStateChange = function(e) {
          var state = e.state, // The state, as manipulated by the user (or set by the URL)
              request = e.request; // Promise of the search request triggered by the state change

          searchPanel.setLoading(true);
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
                resultList.setSelectedItem();
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
                deselectItem(currentSelection);
                break;
            }
        },

        /** Common select functionality **/
        onSelectItem = function(item) {

          var uri = ItemUtils.getURIs(item)[0],

              /**
               * For places, we fetch the total result count at that place (i.e.
               * the total number of items that link to that place).
               */
              selectPlace = function(place) {

                var fetchRelated  = function() {
                      // Transient search, filtered by URI of the place, but without queryphrase
                      var filter = { places: [ uri ] },
                          origQuery = state.getQueryPhrase();

                      state.setQueryPhrase(false, NOOP);
                      return state.updateFilters(filter, { pushState: false })
                        .then(function(results) {
                          // Change back to original settings
                          state.updateFilters({ places: false }, NOOP);
                          state.setQueryPhrase(origQuery, NOOP);
                          return results;
                        });
                    },

                    setSelection = function(results) {
                      var resultsAt = results.total - 1; // We don't want to count the place itself

                          // related = results.top_places.filter(function(p) {
                            // Again, count only the other places
                            // return p.doc_id !== place.doc_id;
                          // });

                      // TODO redundancy with selectObject!
                      state.setSelectedItem(place);
                      resultList.setSelectedItem(place);
                      selectionPanel.show(place, { results: resultsAt, relatedPlaces: [] });
                      // TODO currentSelection = { item: item, references: references }
                      currentSelection = place;

                      // Note: selection may have happend through the map, so technically no
                      // need for this - but the map is designed to handle this situation
                      // map.setSelectedItem(item, references.PLACE);
                    };

                fetchRelated().done(setSelection);
              },

              /**
               * For objects, we fetch their references (e.g. places they link to), plus
               * the total number of other results at that reference.
               */
              selectObject = function(item) {

                var fetchResultCountForReference = function(uri) {
                      var filter = { places : [ uri ] };

                      return state.updateFilters(filter, { pushState: false })
                        .then(function(results) {
                          state.updateFilters({ places: false }, NOOP);
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

                  // TODO redundancy with selectPlace!
                  state.setSelectedItem(item);
                  resultList.setSelectedItem(item);
                  selectionPanel.show(item, { references: references, resultCounts: resultCounts });
                  searchPanel.setLoading(false);

                  // TODO currentSelection = { item: item, references: references }
                  currentSelection = item;

                  // Note: selection may have happend through the map, so technically no
                  // need for this - but the map is designed to handle this situation
                  map.setSelectedItem(item, references.PLACE);
                });
              },

              selectPerson = function(person) {
                // TODO
                selectObject(person);
              },

              selectDataset = function(dataset) {
                // TODO indicate that there is 'source' filter via searchbox footer
                // TODO (same way as if filter were set via facets panel)
                state.clearSearch(NOOP);
                state.updateFilters({ 'datasets' : [ uri ] }, NOOP);

                // Handle this as a transitional request, so we can force a time histgraom
                API.getDatasetInfo(uri).done(function(response) {
                  state.setSelectedItem(dataset);
                  selectionPanel.show(dataset, response);
                  currentSelection = dataset;

                  searchPanel.setSearchResponse(response);
                  resultList.setSearchResponse(response);
                  map.setSearchResponse(response);
                  map.fitBounds();

                  searchPanel.setLoading(false);
                });
              };

          searchPanel.setLoading(true);

          if (item)
            switch(ItemUtils.getItemType(item)) {
              case 'PLACE':
                selectPlace(item);
                break;
              case 'OBJECT':
                selectObject(item);
                break;
              case 'PERSON':
                selectPerson(item);
                break;
              case 'DATASET':
                selectDataset(item);
                break;
            }
          else
            deselect();
        },

        onSelectMapMarker = function(place) {
          if (place) {
            searchPanel.setLoading(true);
            var uri = ItemUtils.getURIs(place)[0],
                filter = { places : [ uri ] };

            return state.updateFilters(filter, { pushState: false })
              .done(function(results) {
                state.updateFilters({ places: false }, { pushState: false, makeRequest: false });
                onSelectItem(results.items[0]);
              });
          } else {
            deselect();
          }
        },

        /** An identifier was selected (e.g. via suggestions) - fetch item **/
        onSelectIdentifier = function(identifier) {
          searchPanel.setLoading(true);
          API.getItem(identifier)
            .done(onSelectItem)
            .fail(function(error) {
              // TODO shouldn't happen unless connection or backend is down
              // TODO show error popup
            });
        },

        onOpenFilterPane = function() {
          searchPanel.setLoading(true);
          state.setFilterPaneOpen(true).done(onSearchResponse);
        },

        onCloseFilterPane = function() {
          state.setFilterPaneOpen(false, { makeRequest: false });
        },

        onQueryPhraseChanged = function(query) {
          searchPanel.setLoading(true);
          selectionPanel.hide();

          // Remove local search/places and dataset filters
          state.updateFilters({
            places   : false,
            datasets : false
          }, NOOP);

          previousGlobalSearch = false;

          state.setQueryPhrase(query).done(function(results) {
            onSearchResponse(results);
            map.fitBounds();
          });
        },

        /**
         * 'Local search' means we'll show ALL items at this place, not just those
         * matching the query phrase. I.e. in addition to setting the filter we also
         * want to clear the phrase from the state.
         *
         * In addition, they payload of the links is different: in onSetFilter, we're
         * dealing with a reference object, whereas in onLocalSearch, we're dealing
         * with the entity object.
         */
        onLocalSearch = function(place) {
          // Convert to key/value format required by state
          var identifier = place.is_conflation_of[0].identifiers[0],
              asFilterSetting = { 'places': [ identifier ]};

          searchPanel.setLoading(true);
          searchPanel.updateFilterCrumbs({ filter: 'places', values: [{
            identifier: identifier,
            label: place.title
          }]});
          selectionPanel.hide();

          // Clear the query phrase - but remember for later
          previousGlobalSearch = state.getQueryPhrase();
          state.setQueryPhrase(false, NOOP);
          state.updateFilters(asFilterSetting).done(function(response) {
            // Exclude the place itself from the response
            response.total = response.total - 1;
            response.items = response.items.filter(function(r) {
              return r.doc_id !== place.doc_id;
            });

            searchPanel.setSearchResponse(response);
            resultList.setSearchResponse(response);
          });
        },

        onSetFilter = function(f) {
          // Convert to key/value format required by state
          var asFilterSetting = {};
          asFilterSetting[f.filter] = f.values.map(function(v) { return v.identifier; });

          searchPanel.setLoading(true);
          searchPanel.updateFilterCrumbs(f);
          selectionPanel.hide();
          state.updateFilters(asFilterSetting).done(onSearchResponse);
        },

        onRemoveAllFilters = function() {
          searchPanel.setLoading(true);

          // TODO leaving local search? restore query phrase
          if (previousGlobalSearch) {
            state.setQueryPhrase(previousGlobalSearch, NOOP);
            previousGlobalSearch = false;
          }

          state.clearFilters().done(onSearchResponse);
        },

        onFilterByViewport = function(filter) {
          searchPanel.setFilterByViewport(filter);
          state.setFilterByViewport(filter).done(onSearchResponse);
        },

        onMapMove = function(bounds) {
          // This may return a new search request, depending on
          // whether the user has enabled filtering by viewport!
          var promise = state.setViewport(bounds);
          if (promise)
            promise.done(onSearchResponse);
        };

    map.on('filterByViewport', onFilterByViewport);
    map.on('selectPlace', onSelectMapMarker);
    map.on('move', onMapMove);

    searchPanel.on('open', onOpenFilterPane);
    searchPanel.on('close', onCloseFilterPane);
    searchPanel.on('queryChange', onQueryPhraseChanged);
    searchPanel.on('selectSuggestOption', onSelectIdentifier);
    searchPanel.on('setFilter', onSetFilter);
    searchPanel.on('removeAllFilters', onRemoveAllFilters);

    // TODO activate load spinner
    searchPanel.on('timerangeChange', seq(state.setTimerange, onSearchResponse));

    selectionPanel.on('select', onSelectIdentifier);
    selectionPanel.on('setFilter', onSetFilter);
    selectionPanel.on('localSearch', onLocalSearch);

    resultList.on('select', onSelectItem);
    resultList.on('nextPage', seq(state.loadNextPage, resultList.appendPage));

    state.on('stateChange', onStateChange);
    state.init();
  });

});
