define([], function() {

  var NOOP = { pushState: false, makeRequest: false };

  var BaseSelectActions = function(selectionPanel, resultList, state) {
    this.selectionPanel = selectionPanel;
    this.resultList = resultList;
    this.state = state;
  };

  /** Shorthand for setting selection across multiple UI components **/
  BaseSelectActions.prototype.setSelected = function(item, args) {
    this.state.setSelectedItem(item);
    this.resultList.setSelectedItem(item);
    this.selectionPanel.show(item, args);
  };

  BaseSelectActions.prototype.deselect = function(item, args) {
    this.selectionPanel.hide();
    this.resultList.setSelectedItem(false);
  };

  /**
   * Fetches items referencing this item, along with those items that are 'related' to it,
   * i.e. those that co-occur in other items references.
   */
  BaseSelectActions.prototype.fetchReferencingAndRelated = function(item) {
    // Transient search, filtered by URI of the place, but without queryphrase
    var state = this.state,
        uri = item.is_conflation_of[0].identifiers[0],
        filter = { referencing: [ uri ] },
        origQuery = state.getQueryPhrase();

    state.setQueryPhrase(false, NOOP);
    return state.updateFilters(filter, { pushState: false })
      .then(function(results) {
        // Restore original settings
        state.updateFilters({ referencing: false }, NOOP);
        state.setQueryPhrase(origQuery, NOOP);
        return results;
      });
  };

  BaseSelectActions.prototype.NOOP = NOOP;

  return BaseSelectActions;

});
