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
        stashedQuery = false,

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
                state.updateFilters({ referencing : false });
                deselectItem(item);
              },

              deselectDataset = function(dataset) {
                deselectItem(dataset);
                if (stashedQuery) {
                  state.setQueryPhrase(stashedQuery, NOOP);
                  stashedQuery = false;
                  state.updateFilters({ datasets: false }).done(onSearchResponse);
                }
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
                deselectDataset(currentSelection);
                break;
            }
        },

        /** Common select functionality **/
        onSelectItem = function(item, opt_via_ref) {

          var uri = ItemUtils.getURIs(item)[0],

              /**
               * For objects, we fetch the items they references, plus
               * the total number of other results at that referenced item.
               */
              selectObject = function(item) {

                var fetchResultCountForReference = function(uri) {
                      var filter = { referencing : [ uri ] };

                      return state.updateFilters(filter, { pushState: false })
                        .then(function(results) {
                          state.updateFilters({ referencing: false }, NOOP);
                          return { 'identifier' : uri, 'resultCount' : results.total };
                        });
                    },

                    fetchRelated = API.getTopReferenced(uri).then(function(referenced) {
                      // Run filtered searches for the first two related items of each type,
                      // so we can display info in the UI
                      var places  = (referenced.PLACE)  ? referenced.PLACE.slice(0, 1)  : false,
                          people  = (referenced.PERSON) ? referenced.PERSON.slice(0, 1) : false,
                          periods = (referenced.PERIOD) ? referenced.PERIOD.slice(0, 1) : false,

                          fRelatedCounts; // TODO support person and period references

                      if (places) {
                        fRelatedCounts = places.map(function(item) {
                          var identifiers = ItemUtils.getURIs(item);
                          return fetchResultCountForReference(identifiers[0]);
                        });

                        // TODO this doesn't seem to work as expected!
                        return jQuery.when.apply(jQuery, fRelatedCounts).then(function() {
                          return { referenced: referenced, referenceCounts: arguments };
                        });
                      } else {
                        return jQuery.Deferred().resolve(this).then(function() {
                          return { referenced: related, referenceCounts: [] };
                        });
                      }
                    });

                fetchRelated.done(function(response) {
                  state.setSelectedItem(item);
                  resultList.setSelectedItem(item);

                  selectionPanel.show(item, jQuery.extend({}, response, {
                    query_phrase : state.getQueryPhrase(),
                    selected_via : opt_via_ref
                  }));

                  searchPanel.setLoading(false);

                  // TODO currentSelection = { item: item, references: references }
                  currentSelection = item;

                  // Note: selection may have happend through the map, so technically no
                  // need for this - but the map is designed to handle this situation
                  map.setSelectedItem(item, response.referenced.PLACE);
                });
              },

              /**
               * For places, we fetch the total result count at that place (i.e.
               * the total number of items that reference that place).
               */
              selectPlace = function(place) {

                var fetchRelated  = function() {
                      // Transient search, filtered by URI of the place, but without queryphrase
                      var filter = { referencing: [ uri ] },
                          origQuery = state.getQueryPhrase();

                      state.setQueryPhrase(false, NOOP);
                      return state.updateFilters(filter, { pushState: false })
                        .then(function(results) {
                          // Change back to original settings
                          state.updateFilters({ referencing: false }, NOOP);
                          state.setQueryPhrase(origQuery, NOOP);
                          return results;
                        });
                    },

                    setSelection = function(results) {
                      // TODO redundancy with selectObject!
                      state.setSelectedItem(place);
                      resultList.setSelectedItem(place);
                      selectionPanel.show(place, { results: results.total, relatedPlaces: results.top_places });
                      // TODO currentSelection = { item: item, references: references }
                      currentSelection = place;

                      // Note: selection may have happend through the map, so technically no
                      // need for this - but the map is designed to handle this situation
                      // map.setSelectedItem(item, references.PLACE);
                    };

                fetchRelated().done(setSelection);
              },

              selectPerson = function(person) {
                // selectObject(person);

                // TODO total redundancy with selectPlace - clean up
                var fetchRelated  = function() {
                      // Transient search, filtered by URI of the person, but without queryphrase
                      var filter = { referencing: [ uri ] },
                          origQuery = state.getQueryPhrase();

                      state.setQueryPhrase(false, NOOP);
                      return state.updateFilters(filter, { pushState: false })
                        .then(function(results) {
                          // Change back to original settings
                          state.updateFilters({ referencing: false }, NOOP);
                          state.setQueryPhrase(origQuery, NOOP);
                          return results;
                        });
                    },

                    setSelection = function(results) {
                      // TODO redundancy with selectObject!
                      state.setSelectedItem(person);
                      resultList.setSelectedItem(person);
                      selectionPanel.show(person, { results: results.total });
                      // TODO currentSelection = { item: item, references: references }
                      currentSelection = person;

                      // Note: selection may have happend through the map, so technically no
                      // need for this - but the map is designed to handle this situation
                      // map.setSelectedItem(item, references.PLACE);
                    };

                fetchRelated().done(setSelection);

              },

              selectPeriod = function(period) {
                // TODO redundancy with selectObject!
                state.setSelectedItem(period);
                resultList.setSelectedItem(period);
                selectionPanel.show(period);
                // TODO currentSelection = { item: item, references: references }
                currentSelection = period;
              },

              selectDataset = function(dataset) {
                // Stash the query so we can return
                stashedQuery = state.getQueryPhrase();

                // Update filter crumbs
                searchPanel.updateFilterCrumbs({ filter: 'datasets', values: [{
                  identifier: uri,
                  label: dataset.title
                }]});

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
              case 'OBJECT':
                selectObject(item);
                break;
              case 'PLACE':
                selectPlace(item);
                break;
              case 'PERSON':
                selectPerson(item);
                break;
              case 'PERIOD':
                selectPeriod(item);
                break;
              case 'DATASET':
                selectDataset(item);
                break;
            }
          else
            deselect();
        },

        onSelectMapMarker = function(place) {

          var selectPlace = function() {
                onSelectItem(place);
              },

              selectFirstResultAt = function() {
                searchPanel.setLoading(true);
                var uri = ItemUtils.getURIs(place)[0],
                    filter = { referencing : [ uri ] };

                return state.updateFilters(filter, { pushState: false })
                  .done(function(results) {
                    state.updateFilters({ referencing: false }, NOOP);
                    onSelectItem(results.items[0], place);
                  });
              };

          if (place)
            if (place.referenced_count.total === 0)
              selectPlace();
            else
              selectFirstResultAt();
          else
            deselect();
        },

        /** An identifier was selected (e.g. via suggestions) - fetch item **/
        onSelectIdentifier = function(identifier) {
          searchPanel.setLoading(true);
          API.getItem(identifier)
            .done(function(response) {
              // The shorter alternative would be .done(onSelectItem).
              // But then the AJAX API response would adds a second call arg ("success")
              // which onSelectItem would mis-interpret as 'via' argument
              onSelectItem(response);
            }).fail(function(error) {
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

          // If there's a stashed query, it's no longer relevant
          stashedQuery = false;

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
        onLocalSearch = function(item) {
          // Convert to key/value format required by state
          var identifier = item.is_conflation_of[0].identifiers[0],
              itemType = ItemUtils.getItemType(item),
              asFilterSetting = { referencing: [ identifier ]};

          searchPanel.setLoading(true);
          searchPanel.updateFilterCrumbs({ filter: 'referencing', values: [{
            identifier: identifier,
            label: item.title,
            type: itemType
          }]});
          selectionPanel.hide();

          // Clear the query phrase - but remember for later
          stashedQuery = state.getQueryPhrase();
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

          if (stashedQuery) {
            state.setQueryPhrase(stashedQuery, NOOP);
            stashedQuery = false;
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
    map.on('changeBasemap', state.setBasemap);

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
