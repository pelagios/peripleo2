define([
  'ui/navigation/selecting/item/baseActions',
  'ui/api'
], function(BaseActions, API) {

  var DatasetActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var self = this,

        select = function(dataset) {
          var uri = dataset.is_conflation_of[0].identifiers[0];

          // Stash the query so we can return
          stashedQuery.set(state.getQueryPhrase());

          // Update filter crumbs
          searchPanel.updateFilterCrumbs({ filter: 'datasets', values: [{
            identifier: uri,
            label: dataset.title
          }]});

          state.clearSearch(self.NOOP);
          state.updateFilters({ 'datasets' : [ uri ] }, self.NOOP);

          // Handle this as a transitional request, so we can force a time histgraom
          API.getDatasetInfo(uri).done(function(response) {
            // Update selection
            selectionPanel.show(dataset, response);

            // Update UI with filtered-by-dataset search response
            resultList.setSearchResponse(response);
            searchPanel.setSearchResponse(response);
            map.setSearchResponse(response);
            map.fitBounds();

            searchPanel.setLoading(false);
          });
        },

        deselect = function() {
          // Default selection removal
          BaseActions.prototype.deselect.call(this);

          // Restore stashed query
          if (stashedQuery.isSet()) {
            state.setQueryPhrase(stashedQuery.get(), self.NOOP);
            stashedQuery.clear();
          }

          // Remove the dataset filter and restore search state
          searchPanel.removeFilterCrumbs('datasets');
          state.updateFilters({ datasets: false }).done(function(response) {
            searchPanel.setSearchResponse(response);
            resultList.setSearchResponse(response);
            map.setSearchResponse(response);
            searchPanel.setLoading(false);
          });
        };

    this.select = select;
    this.deselect = deselect;

    BaseActions.apply(this, [ selectionPanel, resultList, state ]);
  };
  DatasetActions.prototype = Object.create(BaseActions.prototype);

  return DatasetActions;

});
