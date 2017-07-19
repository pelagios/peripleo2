define(['ui/navigation/selecting/item/baseActions'], function(BaseActions) {

  var PlaceActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var self = this,

        select = function(place) {
          self.fetchRelated(place).done(function(results) {
            self.setSelected(place, { results: results.total, relatedPlaces: results.top_places });
            searchPanel.setLoading(false);

            // Note: selection may have happend through the map, so technically no
            // need for this - but the map is designed to handle this situation
            // map.setSelectedItem(item, references.PLACE);
          });
        },

        deselect = function(place) {
          // TODO check if there's a filter set at all?
          this.state.updateFilters({ referencing : false });
          BaseActions.prototype.deselect.call(this);
        };

    this.select = select;
    this.deselect = deselect;

    BaseActions.apply(this, [ selectionPanel, resultList, state ]);
  };
  PlaceActions.prototype = Object.create(BaseActions.prototype);

  return PlaceActions;

});
