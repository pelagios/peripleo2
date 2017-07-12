define([
  'ui/common/hasEvents',
  'ui/controls/search/filtercrumbs/crumb'
], function(HasEvents, Crumb) {

  var SLIDE_DURATION = 200,

      /** Helper to compute array diff (a - b) **/
      diff = function(a, b) {
        return a.filter(function(x) { return b.indexOf(x) < 0; });
      };

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

        findFilterCrumb = function(filter, value) {
          return crumbs.find(function(crumb) {
            console.log(crumb);
            return crumb.matches(filter, value);
          });
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
                toExpand.forEach(function(c) { c.expand(); });
                toCollapse.forEach(function(c) { c.collapse(); });

                if (!element.is(':visible'))
                  element.velocity('slideDown', { duration: SLIDE_DURATION });
              },

              toExpand = [], toCollapse = [],  toAdd = [];

          filterSetting.values.forEach(function(value) {
            var existingCrumb = findFilterCrumb(filter, value);
            if (existingCrumb)
              // The crumb for this filter exists already - expand
              toExpand.push(existingCrumb);
            else
              // Add a new crumb
              toAdd.push(new Crumb(list, filter, value));
          });

          // Compute toCollapse, update crumbs array and render
          toCollapse = diff(crumbs, toExpand);
          crumbs = crumbs.concat(toAdd);
          show();
        },

        /** Clears all filter crumbs **/
        clearAll = function() {
          if (element.is(':visible'))
            element.velocity('slideUp', {
              duration: SLIDE_DURATION,

              // TODO destroy filter crumbs
              complete: function() { list.empty(); }
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
