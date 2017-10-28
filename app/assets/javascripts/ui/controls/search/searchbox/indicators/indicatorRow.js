define([
  'ui/controls/search/searchbox/indicators/indicator'
], function(Indicator) {

  var SLIDE_DURATION = 200,

      /**
       * Helper to compute array diff (a - b). Needed to keep the indicators unique in the list.
       */
      diff = function(arr, b) {
        if (jQuery.isArray(b))
          return arr.filter(function(x) { return b.indexOf(x) < 0; });
        else
          return arr.filter(function(x) {  return x !== b; });
      };

  var IndicatorRow = function(searchForm, searchInput) {

    var element = jQuery(
          '<ul class="filter-indicators"></ul>').prependTo(searchForm),

        timefilterIndicator = jQuery(
          '<li class="closed timerange"><span class="icon">&#xf017;</span></li>').hide().appendTo(element),

        initialInputPadding = parseInt(searchInput.css('padding-left')),

        indicators = [],

        findIndicator = function(filter, value) {
          return indicators.find(function(i) {
            return i.matches(filter, value);
          });
        },

        refreshInputPadding = function() {
          var rowWidth = element.innerWidth();
          if (indicators.length > 0)
            searchInput.css('padding-left', rowWidth - 4 + initialInputPadding);
          else
            searchInput.css('padding-left', rowWidth + initialInputPadding);
        },

        isEmpty = function() {
          return indicators.length === 0;
        },

        /**
         * Adds a filter setting to the list. Reminder:
         * a filter setting is a combination of one filter type, and
         * one or more filter values. Therefore, one setting may
         * correspond to multiple crumbs.
         */
        update = function(filterSetting) {
          var filter = filterSetting.filter,

              toExpand = [], toCollapse = [],  toAdd = [],

              show = function() {
                toExpand.forEach(function(i) { i.expand(refreshInputPadding); });
                toCollapse.forEach(function(i) { i.collapse(refreshInputPadding); });
                refreshInputPadding();
              };

          filterSetting.values.forEach(function(value) {
            var existingIndicator = findIndicator(filter, value);
            if (existingIndicator)
              // The indicator for this filter exists already - expand
              toExpand.push(existingIndicator);
            else
              // Add a new indicator
              toAdd.push(new Indicator(element, filter, value));
          });

          // Compute toCollapse, update indicator array and render
          toCollapse = diff(indicators, toExpand);
          indicators = indicators.concat(toAdd);
          show();
        },

        /** Removes one specific filter (or all of a specific type) **/
        remove = function(filterType, opt_identifier) {
          var toRemove = indicators.find(function(i) {
                return i.hasType(filterType);
              });

          if (toRemove) {
            // Remove crumb from array
            indicators.splice(indicators.indexOf(toRemove), 1);

            // Remove LI element from list
            toRemove.element.remove();
          }
        },

        showTimefilterIndicator = function(interval) {
          timefilterIndicator.show();
          refreshInputPadding();
        },

        hideTimefilterIndicator = function() {
          timefilterIndicator.hide();
          refreshInputPadding();
        },

        /** Clears all filter indicators **/
        clear = function() {
          element.find('li').not('.timerange').remove();
          indicators = [];
          refreshInputPadding();
        },

        onClick = function(e) {
          var li = jQuery(e.target).closest('li'),

              indicator = indicators.find(function(i) {
                return i.isAttachedTo(li);
              });

          if (indicator) {
            if (indicator.isCollapsed()) {
              // Expand this indicator and collapse all others
              indicator.expand(refreshInputPadding);
              diff(indicators, indicator).forEach(function(i) {
                i.collapse(refreshInputPadding);
              });
            } else {
              indicator.collapse(refreshInputPadding);
            }
          }
        };

    element.on('click', 'li', onClick);

    this.clear = clear;
    this.isEmpty = isEmpty;
    this.update = update;
    this.remove = remove;
    this.showTimefilterIndicator = showTimefilterIndicator;
    this.hideTimefilterIndicator = hideTimefilterIndicator;
  };

  return IndicatorRow;

});
