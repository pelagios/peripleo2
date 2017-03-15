define(['ui/common/hasEvents'], function(HasEvents) {

  // TODO does it make sense to have a common Layer parent class?

  var ItemLayer = function(map) {

    var update = function(items) {

        };

    this.update = update;

    HasEvents.apply(this);
  };
  ItemLayer.prototype = Object.create(HasEvents.prototype);

  return ItemLayer;

});
