define([
  'ui/common/itemUtils',
  'ui/navigation/selecting/item/datasetActions',
  'ui/navigation/selecting/item/objectActions',
  'ui/navigation/selecting/item/periodActions',
  'ui/navigation/selecting/item/personActions',
  'ui/navigation/selecting/item/placeActions',
  'ui/api'
], function(
  ItemUtils,
  DatasetActions,
  ObjectActions,
  PeriodActions,
  PersonActions,
  PlaceActions,
  API) {

  var SelectActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

        // Keeps track of current selection, so we can deselect
    var currentSelection = false,

        // Splitting into sub-objects for better readability
        datasetActions =
          new DatasetActions(map, searchPanel, selectionPanel, resultList, state, stashedQuery),
        objectActions =
          new ObjectActions(map, searchPanel, selectionPanel, resultList, state, stashedQuery),
        periodActions =
          new PeriodActions(map, searchPanel, selectionPanel, resultList, state, stashedQuery),
        personActions =
          new PersonActions(map, searchPanel, selectionPanel, resultList, state, stashedQuery),
        placeActions =
          new PlaceActions(map, searchPanel, selectionPanel, resultList, state, stashedQuery),

        onSelectIdentifier = function(identifier) {
          searchPanel.setLoading(true);
          searchPanel.clearFooter();
          resultList.close();
          map.clear();
          API.getItem(identifier)
            .then(onSelectItem)
            .done(function() {
              map.fitBounds();
            }).fail(function(error) {
              // TODO shouldn't happen unless connection or backend is down
              // TODO show error popup
            });
        },

        onSelectMapMarker = function(place) {

          var selectFirstResultAt = function() {
                searchPanel.setLoading(true);
                var uri = ItemUtils.getURIs(place)[0],
                    filter = { referencing : [ uri ] },
                    idx = resultList.getPosition(place);

                if (idx === 0)
                  // Place at top position - don't even fetch first result
                  onSelectItem(place);
                else
                  state.updateFilters(filter, { pushState: false })
                    .done(function(results) {
                      var firstItem = results.items[0],
                          firstItemIdx = resultList.getPosition(firstItem);

                      state.updateFilters({ referencing: false }, { pushState: false, makeRequest: false });

                      if (idx > -1 && idx < firstItemIdx)
                        // The place is in the list, and higher up than the item
                        onSelectItem(place);
                      else
                        onSelectItem(firstItem, place);
                  });
              };

          if (place)
            if (place.referenced_count.total === 0) onSelectItem(place);
            else selectFirstResultAt();
          else
            deselect();
        },

        onSelectItem = function(item, opt_via) {
          if (item) {
            searchPanel.setLoading(true);
            currentSelection = item;
            switch(ItemUtils.getItemType(item)) {
              case 'DATASET':
                return datasetActions.select(item);
              case 'OBJECT':
                return objectActions.select(item, opt_via);
              case 'PERIOD':
                return periodActions.select(item);
              case 'PERSON':
                return personActions.select(item);
              case 'PLACE':
                return placeActions.select(item);
            }
          } else {
            deselect();
          }
        },

        deselect = function() {
          if (currentSelection)
            // Delegate to the appropriate item-specific implementation
            switch(ItemUtils.getItemType(currentSelection)) {
              case 'DATASET':
                datasetActions.deselect(currentSelection);
                break;
              case 'OBJECT':
                objectActions.deselect(currentSelection);
                break;
              case 'PERIOD':
                periodActions.deselect(currentSelection);
                break;
              case 'PERSON':
                personActions.deselect(currentSelection);
                break;
              case 'PLACE':
                placeActions.deselect(currentSelection);
                break;
            }

            state.setSelectedItem();
            currentSelection = false;
        };

    this.onSelectItem = onSelectItem;
    this.onSelectIdentifier = onSelectIdentifier;
    this.onSelectMapMarker = onSelectMapMarker;
    this.deselect = deselect;
  };

  return SelectActions;

});
