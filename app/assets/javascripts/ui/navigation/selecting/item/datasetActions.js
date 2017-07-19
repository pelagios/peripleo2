define(['ui/navigation/selecting/item/baseActions'], function(BaseActions) {

  var DatasetActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var select = function(dataset) {
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

    this.select = select;

    BaseActions.apply(this, [ selectionPanel, resultList, state ]);
  };
  DatasetActions.prototype = Object.create(BaseActions.prototype);

  return DatasetActions;

});
