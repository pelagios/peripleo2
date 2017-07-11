define([
  'ui/common/hasEvents',
  'ui/controls/search/filtercrumbs/crumb'
], function(HasEvents, Crumb) {

  var SLIDE_DURATION = 200;

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

        update = function(filterSetting) {
          // TODO handle existing filters?
          filterSetting.values.forEach(function(v) {
            new Crumb(filterListEl, filterSetting, v);
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
