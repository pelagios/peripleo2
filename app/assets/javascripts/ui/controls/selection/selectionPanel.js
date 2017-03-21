define(['ui/common/hasEvents'], function(HasEvents) {

  var SelectionPanel = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="current-selection">' +
          '</div>').hide().appendTo(parentEl),

        show = function(item) {
          console.log('showing selection', item);
        };

    this.show = show;

    HasEvents.apply(this);
  };
  SelectionPanel.prototype = Object.create(HasEvents.prototype);

  return SelectionPanel;

});
