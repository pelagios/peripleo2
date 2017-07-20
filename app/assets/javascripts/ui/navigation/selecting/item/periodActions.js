define(['ui/navigation/selecting/item/baseActions'], function(BaseActions) {

  var PeriodActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var self = this,

        select = function(period) {
          self.setSelected(period);
          searchPanel.setLoading(false);
        };

    this.select = select;

    BaseActions.apply(this, [ selectionPanel, resultList, state ]);
  };
  PeriodActions.prototype = Object.create(BaseActions.prototype);

  return PeriodActions;

});
