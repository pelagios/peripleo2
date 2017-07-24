define(['ui/navigation/selecting/item/baseActions'], function(BaseActions) {

  var PlaceActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var self = this,

        select = function(place) {
          return self.fetchReferencingAndRelated(place).done(function(results) {
            var refCount = results.total,

                // Top-referenced places include the place itself - exclude from related places
                related = results.top_referenced.PLACE.filter(function(p) {
                  return p.doc_id != place.doc_id;
                });

            self.setSelected(place,
              { referencingCount: refCount, relatedPlaces: related });

            searchPanel.setLoading(false);

            // Note: selection may have happend through the map, so technically no
            // need for this - but the map is designed to handle this situation
            map.setSelectedItem(place);
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
