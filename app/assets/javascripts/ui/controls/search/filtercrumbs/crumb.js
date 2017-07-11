define([], function() {

  var ICONS = {
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
      getIcon = function(filterType, value) {
        if (filterType === 'referencing')
          // Different icons depending on the type of item reference
          return ICONS[value.type];
        else
          // Otherwise, icon by filter type
          return ICONS[filterType];
      },

      /**
       * If it's a type filter (Objects, Places, People, Periods), then base
       * CSS styling depends on the type of item. Otherwise, base styling just
       * depends on the filter type.
       */
      getCSSClass = function(filterSetting) {
        return (filterSetting.filter === 'types') ?
          'types ' + filterSetting.values[0].label.toLowerCase() : filterSetting.filter;
      };

  var Crumb = function(parentEl, filterSetting, value) {

    var element = jQuery(
          '<li class="' + getCSSClass(filterSetting) + '">' +
            '<span class="icon">' + getIcon(filterSetting.filter, value) + '</span>' +
            '<spann class="label">' + value.label + '</span>' +
          '</li>').appendTo(parentEl);

  };

  return Crumb;

});
