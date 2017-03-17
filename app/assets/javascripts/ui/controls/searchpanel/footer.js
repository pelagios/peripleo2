define([
  'ui/common/formatting',
  'ui/common/hasEvents'
], function(Formatting, HasEvents) {

  var ROTATE_DURATION = 250;

  var PanelFooter = function(parentEl) {

    var self = this,

        footer = jQuery(
          '<div id="filterpane-footer">' +
            '<span class="footer-text">' +
              '<span class="icon">&#xf03a;</span>' +
              '<span class="label"></span>' +
            '</span>' +
            '<span class="pane-toggle"></span>' +
          '</div>').appendTo(parentEl),

        label = footer.find('.label'),

        btnTogglePane = footer.find('.pane-toggle'),

        onTogglePane = function() {
          var isOpen = btnTogglePane.hasClass('open');

          if (isOpen) {
            btnTogglePane.removeClass('open');
            btnTogglePane.velocity({ rotateZ: '0deg' }, { duration: ROTATE_DURATION });
          } else {
            btnTogglePane.addClass('open');
            btnTogglePane.velocity({ rotateZ: '-180deg' }, { duration: ROTATE_DURATION });
          }

          self.fireEvent('toggle');
        },

        update = function(response) {
          label.html(Formatting.formatNumber(response.total) + ' results');
        };

    btnTogglePane.click(onTogglePane);

    this.update = update;

    HasEvents.apply(this);
  };
  PanelFooter.prototype = Object.create(HasEvents.prototype);

  return PanelFooter;

});
