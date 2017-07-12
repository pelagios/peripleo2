define([
  'ui/common/hasEvents',
  'ui/controls/search/filtercrumbs/crumb'
], function(HasEvents, Crumb) {

  var SLIDE_DURATION = 200;

  var FilterCrumbs = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div class="filtercrumbs">' +
            '<div class="fc-label icon">&#xf0b0;</div>' +
            '<ul class="fc-filters"></ul>' +
            '<div class="fc-clear icon stroke7">&#xe680;</div>' +
          '</div>').hide().appendTo(parentEl),

        list        = element.find('.fc-filters'),
        btnClearAll = element.find('.fc-clear'),

        crumbs = [],

        /** Returns true if the filter is already in the list **
        findFilter = function(type, identifier) {
          return filters.find(function(f) {
            return f.type === type && f.identifier === identifier;
          });
        },*/

        existsFilterCrumb = function(filter, value) {
          // TODO implement
          return false;
        },

        /**
         * Adds a filter setting to the list. Reminder:
         * a filter setting is a combination of one filter type, and
         * one or more filter values. Therefore, one setting may
         * correspond to multiple crumbs.
         */
        update = function(filterSetting) {
          var filter = filterSetting.filter,

              show = function() {
                if (!el.is(':visible'))
                  el.velocity('slideDown', { duration: SLIDE_DURATION });
              };

          filterSetting.values.forEach(function(value) {
            if (!existsFilterCrumb(filter, value))
              crumbs.push(new Crumb(filterListEl, filter, value));
          }, []);

          show();
        },

        /** Clears all filter crumbs **/
        clearAll = function() {

          // TODO destroy filter crumbs

          if (el.is(':visible'))
            el.velocity('slideUp', {
              duration: SLIDE_DURATION,
              complete: function() { filterListEl.empty(); }
            });

          self.fireEvent('removeAll');
        };

    btnClearAll.click(clearAll);

    this.update = update;

    HasEvents.apply(this);
  };
  FilterCrumbs.prototype = Object.create(HasEvents.prototype);

  return FilterCrumbs;

});
