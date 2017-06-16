define(['ui/common/hasEvents'], function(HasEvents) {

  var SLIDE_DURATION = 100,

      ICONS = {
        'PLACE'   : '&#xf041;',
        'OBJECT'  : '&#xf219;',
        'PERSON'  : '&#xf007;',
        'DATASET' : '&#xf187;'
      };

      TYPE_LABELS = {
        'PLACE'   : 'Places',
        'OBJECT'  : 'Objects',
        'PERSON'  : 'People',
        'DATASET' : 'Datasets'
      };

  var FilterCrumbs = function(parentEl) {

    var self = this,

        el = jQuery(
          '<div class="rl-h-container">' +
            '<div class="rl-h-label icon">&#xf0b0;</div>' +
            '<ul class="rl-h-filters"></ul>' +
            '<div class="rl-h-clear icon stroke7">&#xe680;</div>' +
          '</div>').appendTo(parentEl),

        filterListEl = el.find('.rl-h-filters'),
        btnClear   = el.find('.rl-h-clear'),

        // Filters are described by { type:..., identifier:..., el: ... }
        filters = [],

        /** Returns true if the filter is already in the list **/
        findFilter = function(type, identifier) {
          return filters.find(function(f) {
            return f.type === type && f.identifier === identifier;
          });
        },

        show = function() {
          if (!parentEl.is(':visible'))
            parentEl.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        hide = function() {

        },

        // TODO just a hack for now
        update = function(diff) {
          if (diff.types) diff.types.forEach(addTypeFilter);
        },

        addTypeFilter = function(type) {
          filterListEl.append(
            '<li class="item-type ' + type + '">' +
              '<span class="icon">' + ICONS[type] + '</span>' +
              '<span class="label">' + TYPE_LABELS[type] + '</span>' +
            '</li>');
          show();
        },

        addFilter = function(type, identifier, label) {
          filterListEl.append(
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
              complete: function() { filterListEl.empty(); }
            });

            self.fireEvent('removeAll');
          }
        };

    btnClear.click(clear);

    this.update = update;
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
