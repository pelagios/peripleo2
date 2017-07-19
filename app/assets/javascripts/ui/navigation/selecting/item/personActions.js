define(['ui/navigation/selecting/item/baseActions'], function(BaseActions) {

  var PersonActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var self = this,

        select = function(person) {
          self.fetchRelated(person).done(function(results) {
            self.setSelected(person, { results: results.total });
            searchPanel.setLoading(false);
          });
        };

    this.select = select;

    BaseActions.apply(this, [ selectionPanel, resultList, state ]);
  };
  PersonActions.prototype = Object.create(BaseActions.prototype);

  return PersonActions;

});
