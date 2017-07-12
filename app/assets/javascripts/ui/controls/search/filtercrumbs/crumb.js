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

      /**
       * If it's a type filter (Objects, Places, People, Periods), then base
       * CSS styling depends on the type of item. Otherwise, base styling just
       * depends on the filter type.
       */
      getCSSClass = function(filter, value) {
        return (filter === 'types') ?
          'types ' + value.label.toLowerCase() : filter;
      };

  var Crumb = function(parentEl, filter, value) {

    var element = jQuery(
          '<li class="' + getCSSClass(filter, value) + '">' +
            '<span class="icon">' + getIcon(filter, value) + '</span>' +
            '<span class="label"><span class="label-inner">' + value.label + '</span></span>' +
          '</li>').appendTo(parentEl),

        label = element.find('.label'),

        /**
         * To be determined before collapsing happens. Otherwise label.width() can be 0
         * directly after attaching to parentEl.
          */
        width,

        /** Returns true if this crumb corresponds to the specified filter/value combination **/
        matches = function(f, v) {
          return f === filter && v.identifier === value.identifier;
        },

        isCollapsed = function() {
          return label.width() === 0;
        },

        collapse = function() {
          if (!isCollapsed())
            width = label.width();
            label.animate({ 'width' : 0 }, COLLAPSE_DURATION);
        },

        expand = function() {
          if (isCollapsed())
            label.animate({ 'width': width }, COLLAPSE_DURATION);
        };

    this.collapse = collapse;
    this.expand = expand;
    this.matches = matches;
  };

  return Crumb;

});
