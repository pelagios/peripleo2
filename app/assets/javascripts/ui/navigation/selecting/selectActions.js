define([
  'ui/common/itemUtils',
  'ui/navigation/selecting/item/datasetActions',
  'ui/navigation/selecting/item/objectActions',
  'ui/navigation/selecting/item/periodActions',
  'ui/navigation/selecting/item/personActions',
  'ui/navigation/selecting/item/placeActions'
], function(ItemUtils, DatasetActions, ObjectActions, PeriodActions, PersonActions, PlaceActions) {

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
          API.getItem(identifier)
            .done(function(response) {
              // The shorter alternative would be .done(onSelectItem).
              // But then the AJAX API response would add a second call arg ("success")
              // which onSelectItem would mis-interpret as 'via' argument
              onSelectItem(response);
            }).fail(function(error) {
              // TODO shouldn't happen unless connection or backend is down
              // TODO show error popup
            });
        },

        onSelectMapMarker = function(place) {

          var selectFirstResultAt = function() {
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
            if (place.referenced_count.total === 0) onSelectItem(place);
            else selectFirstResultAt();
          else
            deselect();
        },

        onSelectItem = function(item) {
          if (item) {
            searchPanel.setLoading(true);

            switch(ItemUtils.getItemType(item)) {
              case 'DATASET':
                datasetActions.select(item);
                break;
              case 'OBJECT':
                objectActions.select(item);
                break;
              case 'PERIOD':
                periodActions.select(item);
                break;
              case 'PERSON':
                personActions.select(item);
                break;
              case 'PLACE':
                placeActions.select(item);
                break;
            }

            currentSelection = item;
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

            currentSelection = false;
        };

    this.onSelectItem = onSelectItem;
    this.onSelectIdentifier = onSelectIdentifier;
    this.onSelectMapMarker = onSelectMapMarker;
    this.deselect = deselect;
  };

  return SelectActions;

});
