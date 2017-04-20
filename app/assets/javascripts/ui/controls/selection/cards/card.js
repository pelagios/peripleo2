define([
  'ui/common/formatting',
  'ui/common/hasEvents'
], function(Formatting, HasEvents) {

  var Card  = function() {

    var formatTemporalBounds = function(bounds) {
          if (bounds.from === bounds.to)
            return Formatting.formatYear(bounds.from);
          else
            return Formatting.formatYear(bounds.from) + ' - ' + Formatting.formatYear(bounds.to);
        };

    this.formatTemporalBounds = formatTemporalBounds;
    
    HasEvents.apply(this);
  };
  Card.prototype = Object.create(HasEvents.prototype);

  return Card;

});
