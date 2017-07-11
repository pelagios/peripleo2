define(['ui/common/hasEvents'], function(HasEvents) {

  var SLIDE_DURATION = 200,

      ICONS = {
        // 'referencing' filter sub-types
        'PLACE'  : '&#xf041;',
        'PERSON' : '&#xf007;',

        // other filters
        'types'   : '&#xf03a;',
        'datasets': '&#xf187;'
      },

      getIcon = function(filterType, value) {
        if (filterType === 'referencing')
          // Different icons depending on the type of item reference
          return ICONS[value.type];
        else
          // Otherwise, icon by filter type
          return ICONS[filterType];
      };

  var FilterCrumbs = function(parentEl) {

    var self = this,

        el = jQuery(
          '<div class="filtercrumbs">' +
            '<div class="fc-label icon">&#xf0b0;</div>' +
            '<ul class="fc-filters"></ul>' +
            '<div class="fc-clear icon stroke7">&#xe680;</div>' +
          '</div>').hide().appendTo(parentEl),

        filterListEl = el.find('.fc-filters'),
        btnClear   = el.find('.fc-clear'),

        // Filters are described by { type:..., identifier:..., el: ... }
        filters = [],

        /** Returns true if the filter is already in the list **/
        findFilter = function(type, identifier) {
          return filters.find(function(f) {
            return f.type === type && f.identifier === identifier;
          });
        },

        show = function() {
          if (!el.is(':visible'))
            el.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        hide = function() {

        },

        update = function(f) {
          var cssClass = (f.filter === 'types') ?
                'types ' + f.values[0].label.toLowerCase() : f.filter;

          // TODO handle existing filters?
          f.values.forEach(function(v) {
            filterListEl.append(
              '<li class="' + cssClass + '">' +
                '<span class="icon">' + getIcon(f.filter, v) + '</span>' +
                '<spann class="label">' + v.label + '</span>' +
              '</li>');
          });
          show();
        },

        clear = function() {
          if (el.is(':visible')) {
            el.velocity('slideUp', {
              duration: SLIDE_DURATION,
              complete: function() { filterListEl.empty(); }
            });

            self.fireEvent('removeAll');
          }
        };

    btnClear.click(clear);

    this.update = update;
    this.clear = clear;

    HasEvents.apply(this);
  };
  FilterCrumbs.prototype = Object.create(HasEvents.prototype);

  return FilterCrumbs;

});