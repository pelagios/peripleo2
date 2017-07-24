define([
  'ui/common/formatting',
  'ui/common/hasEvents'
], function(Formatting, HasEvents) {

  // Time (milliseconds) for the rotate anim of the arrow
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

        icon  = footer.find('.icon'),
        label = footer.find('.label'),
        btnToggleFilterPane = footer.find('.pane-toggle'),

        /** Clears the footer result count **/
        clear = function() {
          label.empty();
        },

        /** Updates the 'filter by viewport' icon **/
        setFilterByViewport = function(filter) {
          if (filter) icon.addClass('by-viewport');
          else icon.removeClass('by-viewport');
        },

        /** Updates the search result count **/
        setSearchResponse = function(response) {
          label.html(Formatting.formatNumber(response.total) + ' results');
        },

        /**
         * Sets the state of the 'toggle pane' icon to open/closed.
         *
         * Note that the footer is passive component. The state of the icon
         * is not connected to the actual state of the filter pane at all.
         * Strings are pulled by filterPane.js.
         */
        setOpen = function(open) {
          var isStateOpen = btnToggleFilterPane.hasClass('open');
          if (open && !isStateOpen) {
            btnToggleFilterPane.addClass('open');
            btnToggleFilterPane.velocity({ rotateZ: '-180deg' }, { duration: ROTATE_DURATION });
          } else if (!open && isStateOpen){
            btnToggleFilterPane.removeClass('open');
            btnToggleFilterPane.velocity({ rotateZ: '0deg' }, { duration: ROTATE_DURATION });
          }
        },

        /**
         * Forwards clicks on the toggle button up the component hierarchy.
         * Note that the footer is a passive component, so this does not affect
         * the state of the icon. (Remember: strings are pulled by filterPane.js)
         */
        onClickToggle = function() {
          self.fireEvent('toggle');
        };

    btnToggleFilterPane.click(onClickToggle);

    this.clear = clear;
    this.setFilterByViewport = setFilterByViewport;
    this.setSearchResponse = setSearchResponse;
    this.setOpen = setOpen;

    HasEvents.apply(this);
  };
  Footer.prototype = Object.create(HasEvents.prototype);

  return Footer;

});
