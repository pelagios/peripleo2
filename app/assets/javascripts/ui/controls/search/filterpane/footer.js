define([
  'ui/common/formatting',
  'ui/common/hasEvents'
], function(Formatting, HasEvents) {

  var ROTATE_DURATION = 250;

  var Footer = function(parentEl) {

    var self = this,

        footer = jQuery(
          '<div id="filterpane-footer">' +
            '<span class="total-result-count">' +
              '<span class="icon"></span>' +
              '<span class="label"></span>' +
            '</span>' +
            '<span class="pane-toggle"></span>' +
          '</div>').appendTo(parentEl),

        icon = footer.find('.icon'),

        label = footer.find('.label'),

        btnTogglePane = footer.find('.pane-toggle'),

        onTogglePane = function(cancelEvent) {
          var isOpen = btnTogglePane.hasClass('open');

          if (isOpen) {
            btnTogglePane.removeClass('open');
            btnTogglePane.velocity({ rotateZ: '0deg' }, { duration: ROTATE_DURATION });
          } else {
            btnTogglePane.addClass('open');
            btnTogglePane.velocity({ rotateZ: '-180deg' }, { duration: ROTATE_DURATION });
          }

          if (cancelEvent !== true) self.fireEvent('toggle');
        },

        setFilterByViewport = function(filter) {
          if (filter) icon.addClass('by-viewport');
          else icon.removeClass('by-viewport');
        },

        setOpen = function(open) {
          var isOpen = btnTogglePane.hasClass('open');
          if (open != isOpen) onTogglePane(true);
        },

        update = function(response) {
          label.html(Formatting.formatNumber(response.total) + ' results');
        };

    btnTogglePane.click(onTogglePane);

    this.update = update;
    this.setOpen = setOpen;
    this.setFilterByViewport = setFilterByViewport;

    HasEvents.apply(this);
  };
  Footer.prototype = Object.create(HasEvents.prototype);

  return Footer;

});
