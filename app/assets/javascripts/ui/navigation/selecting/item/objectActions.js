define(['ui/navigation/selecting/item/baseActions'], function(BaseActions) {

  var ObjectActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var select = function(item) {

        };

    this.select = select;

    /**
    /** Common select functionality **
    onSelectItem = function(item, opt_via_ref) {

      var uri = ItemUtils.getURIs(item)[0],

          /**
           * For objects, we fetch the items they references, plus
           * the total number of other results at that referenced item.
           *
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
                      return { referenced: referenced, referenceCounts: Array.from(arguments) };
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
          };

    };

    */

    BaseActions.apply(this, [ selectionPanel, resultList, state ]);
  };
  ObjectActions.prototype = Object.create(BaseActions.prototype);

  return ObjectActions;

});
