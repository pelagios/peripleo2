define([], function() {

  var FilterActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var updateAll = function(response) {
          searchPanel.setSearchResponse(response);
          resultList.setSearchResponse(response);
          map.setSearchResponse(response);
          searchPanel.setLoading(false);
        },

        /** Opening the filter pane triggers a new search **/
        onOpenFilterPane = function() {
          searchPanel.setLoading(true);
          state.setFilterPaneOpen(true).done(updateAll);
        },

        /** Closing the filter pane just updates state, but doesn't trigger a search **/
        onCloseFilterPane = function() {
          state.setFilterPaneOpen(false, { makeRequest: false });
        },

        onSetFilter = function(f) {
          // Convert to key/value format required by state
          var asFilterSetting = {};
          asFilterSetting[f.filter] = f.values.map(function(v) { return v.identifier; });

          searchPanel.setLoading(true);
          searchPanel.updateFilterCrumbs(f);
          selectionPanel.hide();

          state.updateFilters(asFilterSetting).done(function(results) {
            var hasPlaceFilter = (f.filter === 'referencing') && f.values.find(function(v) {
              return v.type === 'PLACE';
            });

            searchPanel.setLoading(false);

            searchPanel.setSearchResponse(results);
            resultList.setSearchResponse(results);

            // If this search was filtered by PLACE, we don't want to update the map,
            // otherwise it would remove all dots except: i) the place by which the
            // the search was filtered; ii) the related places.
            // Note: we may wan to treat this differently later, perhaps depending on
            // a user setting?
            if (!hasPlaceFilter)
              map.setSearchResponse(results);
          });
        },

        onRemoveAllFilters = function() {
          searchPanel.setLoading(true);

          if (stashedQuery.isSet()) {
            state.setQueryPhrase(stashedQuery.get(), NOOP);
            stashedQuery.clear();
          }

          state.clearFilters().done(updateAll);
        },

        onFilterByViewport = function(filter) {
          searchPanel.setFilterByViewport(filter);
          state.setFilterByViewport(filter).done(updateAll);
        };

    this.onOpenFilterPane = onOpenFilterPane;
    this.onCloseFilterPane = onCloseFilterPane;
    this.onSetFilter = onSetFilter;
    this.onRemoveAllFilters = onRemoveAllFilters;
    this.onFilterByViewport = onFilterByViewport;
  };

  return FilterActions;

});
