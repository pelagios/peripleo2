define([], function() {

  var COLLAPSE_DURATION = 150,

      ICONS = {
        // 'referencing' filter sub-types
        'PLACE'  : '&#xf041;',
        'PERSON' : '&#xf007;',

        // other filters
        'types'   : '&#xf03a;',
        'datasets': '&#xf187;'
      },

      /**
       * If it's a type filter (Objects, Places, People, Periods), then base
       * CSS styling depends on the type of item. Otherwise, base styling just
       * depends on the filter type.
       */
      getCSSClass = function(filter, value) {
        return (filter === 'types') ?
          'types ' + value.label.toLowerCase() : filter;
      },

      /**
       * Returns the appropriate icon for the specified filter type and filter value.
       * (Reminder: a filter can have one type, and multiple values. Example: 'referencing' with
       * a Place and a Person value.)
       */
      getIcon = function(filter, value) {
        if (filter === 'referencing')
          // Different icons depending on the type of item reference
          return ICONS[value.type];
        else
          // Otherwise, icon by filter type
          return ICONS[filter];
      },

      getTooltip = function(filter, value) {
        var label = value.label.replace('\u0007', ' > ');
        if (filter === 'referencing') {
          return 'Show only items linked to ' + label;
        } else if (filter === 'types') {
          return 'Show only ' + label;
        } else if (filter === 'datasets') {
          return 'Show only items from ' + label;
        }
      };

  var Indicator = function(parentEl, filter, value) {
    var element = jQuery(
                   '<li class="open">' +
                     '<span class="icon">' + getIcon(filter, value) + '</span>' +
                     '<span class="label">' +
                       '<span class="label-inner">' +
                         value.label.replace('\u0007', '<span class="separator"></span>') +
                       '</span>' +
                     '</span>' +
                   '</li>').appendTo(parentEl),

        label = element.find('.label'),

        /**
         * To be determined before collapsing happens. Otherwise label.width() can be 0
         * directly after attaching to parentEl.
          */
        width,

        /** Returns true if this indicator corresponds to the specified filter/value combination **/
        matches = function(f, v) {
          return f === filter && v.identifier === value.identifier;
        },

        /** Returns true if this crumb has the specified filter type **/
        hasFilterType = function(t) {
          return t === filter;
        },

        /** Returns true if this crumb has the specified filter type **/
        hasEntityType = function(t) {
          return t === value.type;
        },

        /**
         * A helper to test whether this indicator is attached to the given
         * LI jQuery element. Used by indicatorRow.js to determine the appropriate
         * indicator after a user clicked on an LI. Not the nicest solution... but
         * don't really know a way with less cross-dependency.
         * (Alternative would be to attach the indicator to the LI via .data(), but introduces
         * the same cross-dependency.)
         */
        isAttachedTo = function(li) {
          return li.is(element);
        },

        isCollapsed = function() {
          return label.width() === 0;
        },

        collapse = function(opt_progress) {
          if (!isCollapsed()) {
            if (!width) width = label.outerWidth() + 1;
            label.velocity({
              width: 0
            }, {
              duration: COLLAPSE_DURATION,
              progress: opt_progress,
              complete: function() {
                element.removeClass('open');
                element.addClass('closed');
              }
            });
          }
        },

        expand = function(opt_progress) {
          if (isCollapsed())
            element.addClass('open');
            element.removeClass('closed');

            label.velocity({
              width: width
            }, {
              duration: COLLAPSE_DURATION,
              progress: opt_progress
            });
        },

        destroy = function() {
          element.remove();
        };

    this.element = element;
    this.collapse = collapse;
    this.expand = expand;
    this.isAttachedTo = isAttachedTo;
    this.isCollapsed = isCollapsed;
    this.matches = matches;
    this.hasFilterType = hasFilterType;
    this.hasEntityType = hasEntityType;
    this.destroy = destroy;
  };

  return Indicator;

});
