define(['ui/common/hasEvents'], function(HasEvents) {

  var PanelFooter = function(parentEl) {

    var footer = jQuery(
          '<div id="filterpane-footer">' +
            '<span class="left">' +
              '<span class="icon">&#xf03a;</span>' +
              '<span class="label"></span>' +
            '</span>' +
            '<span class="right"></span>' +
          '</div>').appendTo(parentEl);

    HasEvents.apply(this);
  };
  PanelFooter.prototype = Object.create(HasEvents.prototype);

  return PanelFooter;

});
