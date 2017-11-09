define([
  'ui/common/itemUtils',
  'ui/ldview/linkedDataView',
  'ui/navigation/filtering/filterActions',
  'ui/navigation/selecting/selectActions',
  'ui/navigation/stashedQuery',
  'ui/api'
], function(ItemUtils, LinkedDataView, FilterActions, SelectActions, StashedQuery, API) {

  /** Shorthand for a 'transient query' state update **/
  var NOOP = { pushState: false, makeRequest: false };

  var Navigation = function(map, searchPanel, selectionPanel, resultList, state) {

        /** Keeps track of search query when moving to local search **/
    var stashedQuery = new StashedQuery(),

        /**
         * We split navigation into sub-objects, just for better readability and
         * shorter source files
         */
        filterActions =
          new FilterActions(map, searchPanel, selectionPanel, resultList, state, stashedQuery),
        selectActions =
          new SelectActions(map, searchPanel, selectionPanel, resultList, state, stashedQuery),

        updateAll = function(response) {
          searchPanel.setSearchResponse(response);
          resultList.setSearchResponse(response);
          map.setSearchResponse(response);
          searchPanel.setLoading(false);
        },

        /** Happens on 'Back' button, or on page load **/
        onStateChange = function(e) {
          var state = e.state, // The state, as manipulated by the user (or set by the URL)
              request = e.request, // Promise of the search request triggered by the state change

              /**
               * Helper function to 'resolve' the URIs of referenced entities from a search
               * response, and build the corresponding filterSetting objects.
               * Because the filter is linked to the search response, we can safely
               * assume that the entities will be listed in the top_referenced section.
               * This saves us a (list of) resolution requests against the API!
               */
              resolveReferences = function(uris, top_referenced) {
                var entityTypes = Object.keys(top_referenced),
                    topRefs = [];

                entityTypes.forEach(function(key) {
                  topRefs = topRefs.concat(top_referenced[key]);
                });

                return uris.map(function(uri) {
                  return topRefs.find(function(item) {
                    var identifiers = ItemUtils.getURIs(item);
                    return identifiers.indexOf(uri) > -1;
                  });
                }).map(function(item) {
                  return {
                    filter: 'referencing',
                    values:[{
                      identifier: item.is_conflation_of[0].uri,
                      label: item.title,
                      type: item.item_type[0]
                    }]
                  };
                });
              },

              restore = function() {
                if (request) {
                  // The state change triggered a search requeset - update UI when complete
                  return request.then(function(response) {
                    // If there are 'referencing' filters, resolve them from the response
                    if (state.search.filters.referencing && response.top_referenced) {
                      var filterSettings =
                        resolveReferences(state.search.filters.referencing, response.top_referenced);

                      filterSettings.forEach(searchPanel.updateFilterIndicators);
                    }

                    updateAll(response);
                  });
                } else {
                  // State change to an 'empty search' - clear UI
                  resultList.close();
                  map.clear();
                }
              };

          searchPanel.setLoading(true);
          searchPanel.setState(state);
          map.setState(state);

          // Need to set selection without triggering history recording
          if (state.selection)
            jQuery.when(restore()).then(function() {
              selectActions.onSelectIdentifier(state.selection, true);
            });
          else
            restore();
        },

        /**
         * Depending on whether the query is defined or not, we either
         * trigger a new request, or clear the search.
         */
        onQueryPhraseChanged = function(query) {
          var search = function() {
                searchPanel.setLoading(true);
                state.setQueryPhrase(query).done(function(response) {
                  updateAll(response);
                  map.fitBounds();
                });
              },

              clear = function() {
                state.setQueryPhrase(false, { makeRequest: false });
                resultList.close();
                map.clear();
              };

          // If there's a stashed query, it's no longer relevant
          stashedQuery.clear();

          // Clear the selection (but don't update state - will happen later in one go)
          state.setSelectedItem(false, false);
          selectionPanel.hide();

          if (query) search();
          else clear();
        },

        onTimerangeChange = function(range) {
          searchPanel.setLoading(true);
          state.setTimerange(range).done(updateAll);
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
          searchPanel.updateFilterIndicators({ filter: 'referencing', values: [{
            identifier: identifier,
            label: item.title,
            type: itemType
          }]});
          selectionPanel.hide();

          // Clear the query phrase - but remember for later
          stashedQuery.set(state.getQueryPhrase());
          state.setQueryPhrase(false, NOOP);
          state.updateFilters(asFilterSetting).done(function(response) {
            searchPanel.setLoading(false);
            searchPanel.setSearchResponse(response);
            resultList.setSearchResponse(response);
          });
        },

        onLinkedDataView = function(item) {
          // TODO record this in state/history
          new LinkedDataView(item);
        },

        onMapMove = function(bounds) {
          // This may return a new search request, depending on
          // whether the user has enabled filtering by viewport!
          var promise = state.setViewport(bounds);
          if (promise) promise.done(updateAll);
        },

        onNextPage = function() {
          state.loadNextPage().done(resultList.appendNextPage);
        },

        onClearAll = function() {
          stashedQuery.clear();
          state.clearSearch({ makeRequest: false });
          resultList.close();
          selectionPanel.hide();
          map.clear();
        };

    // Implemented directly in navigation.js
    this.onStateChange = onStateChange;
    this.onQueryPhraseChanged = onQueryPhraseChanged;
    this.onTimerangeChange = onTimerangeChange;
    this.onLocalSearch = onLocalSearch;
    this.onLinkedDataView = onLinkedDataView;
    this.onMapMove = onMapMove;
    this.onNextPage = onNextPage;

    // Split out into filterActions.js for better code readability
    this.onOpenFilterPane = filterActions.onOpenFilterPane;
    this.onCloseFilterPane = filterActions.onCloseFilterPane;
    this.onFilterByViewport = filterActions.onFilterByViewport;
    this.onSetFilter = filterActions.onSetFilter;
    this.onClearAll = onClearAll;

    // Split out into selectActions.js (and sub-components) for better code readability
    this.onSelectItem = selectActions.onSelectItem;
    this.onSelectIdentifier = selectActions.onSelectIdentifier;
    this.onSelectMapMarker = selectActions.onSelectMapMarker;
  };

  return Navigation;

});
