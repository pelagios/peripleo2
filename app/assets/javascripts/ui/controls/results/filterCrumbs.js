define(['ui/common/hasEvents'], function(HasEvents) {

  var SLIDE_DURATION = 100;

  var ICONS = {
    'PLACE' : '&#xf041;'
  };

  var FilterCrumbs = function(parentEl) {

    var self = this,

        el = jQuery(
          '<div class="rl-h-container">' +
            '<div class="rl-h-label icon">&#xf0b0;</div>' +
            '<ul class="rl-h-filters"></ul>' +
            '<div class="rl-h-clear icon stroke7">&#xe680;</div>' +
          '</div>').appendTo(parentEl),

        filterList = el.find('.rl-h-filters'),
        btnClear   = el.find('.rl-h-clear'),

        show = function() {
          if (!parentEl.is(':visible'))
            parentEl.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        hide = function() {

        },

        addFilter = function(type, identifier, label) {
          filterList.append(
            '<li>' +
              '<span class="icon">' + ICONS[type] + '</span>' +
              '<a class="label destination" href="#">' + label + '</a>' +
            '</li>');

          show();
        },

        setCategoryFilter = function(category, opt_identifier) {

        },

        setDatasetFilter = function(identifier, title) {

        },

        setPeriodFilter = function(identifier, title) {

        },

        setPersonFilter = function(identifier, name) {

        },

        setPlaceFilter = function(identifier, title) {
          addFilter('PLACE', identifier, title);
        },

        setTypeFilter = function(type) {

        },

        clear = function() {
          if (parentEl.is(':visible')) {
            parentEl.velocity('slideUp', {
              duration: SLIDE_DURATION,
              complete: function() { filterList.empty(); }
            });

            self.fireEvent('removeAll');
          }
        };

    btnClear.click(clear);

    this.setCategoryFilter = setCategoryFilter;
    this.setDatasetFilter = setDatasetFilter;
    this.setPeriodFilter = setPeriodFilter;
    this.setPersonFilter = setPersonFilter;
    this.setPlaceFilter = setPlaceFilter;
    this.setTypeFilter = setTypeFilter;
    this.clear = clear;

    HasEvents.apply(this);
  };
  FilterCrumbs.prototype = Object.create(HasEvents.prototype);

  return FilterCrumbs;

});
