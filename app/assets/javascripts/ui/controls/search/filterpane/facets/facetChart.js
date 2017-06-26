define([
  'ui/common/hasEvents',
  'ui/common/formatting'
], function(HasEvents, Formatting) {

  var SLIDE_DURATION = 240;

  var TEMPLATE = '<li class="meter"><span class="count"></span><span class="label"></span></li>';

  var FacetChart = function(parentEl) {

    var self = this,

        el = jQuery('<ul></ul>').appendTo(parentEl),

        getLabel = function(path) {
          return path.map(function(segment) {
            return segment.label;
          }).join(' > ');
        },

        createBar = function(bucket, percent) {
          var el = jQuery(TEMPLATE),
              countEl = el.find('.count'),
              labelEl = el.find('.label');

          el.data('path', bucket.path);
          countEl.html(Formatting.formatNumber(bucket.count));
          labelEl.html(getLabel(bucket.path));
          labelEl.css('width', 0.6 * percent + '%');
          return el;
        },

        toggle = function() {
          if (parentEl.is(':visible'))
            parentEl.velocity('slideUp', { duration: SLIDE_DURATION });
          else
            parentEl.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        update = function(buckets) {
          var maxCount = (buckets.length > 0) ? buckets[0].count : 0;

          el.empty();
          buckets.forEach(function(bucket) {
            var pcnt = 100 * bucket.count / maxCount;
            el.append(createBar(bucket, pcnt));
          });
        },

        onSetFilter = function(e) {
          var li = jQuery(e.target).closest('li'),
              path = li.data('path');

          self.fireEvent('setFilter', {
            filter: 'datasets', // TODO just a quick hack
            values: [{
              identifier: path[path.length - 1].id,
              label: getLabel(path)
            }]
          });
        };

    parentEl.on('click', 'ul', onSetFilter);

    this.toggle = toggle;
    this.update = update;

    HasEvents.apply(this);
  };
  FacetChart.prototype = Object.create(HasEvents.prototype);

  return FacetChart;

});
