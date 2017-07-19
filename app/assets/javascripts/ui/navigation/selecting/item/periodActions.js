define(['ui/navigation/selecting/item/baseActions'], function(BaseActions) {

  var PeriodActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var select = function(period) {
          // TODO redundancy with selectObject!
          state.setSelectedItem(period);
          resultList.setSelectedItem(period);
          selectionPanel.show(period);
          // TODO currentSelection = { item: item, references: references }
          currentSelection = period;
        };

    this.select = select;

    BaseActions.apply(this, [ selectionPanel, resultList, state ]);
  };
  PeriodActions.prototype = Object.create(BaseActions.prototype);

  return PeriodActions;

});
