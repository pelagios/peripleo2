define([
  'ui/common/hasEvents',
  'ui/common/formatting'
], function(HasEvents, Formatting) {

  var SLIDE_DURATION = 240;

  var TEMPLATE = '<tr><td class="count"></td><td class="label"></td></tr>';

  var FacetChart = function(parentEl) {

    var self = this,

        el = jQuery(
          '<div>' +
            '<div class="donut">' +
              '<svg width="100%" height="100%" viewBox="0 0 42 42" class="donut">' +
                '<circle class="donut-hole" cx="21" cy="21" r="15.91549430918954" fill="#f5f7f7"></circle>' +
                '<circle class="donut-ring" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#d8d9db" stroke-width="5"></circle>' +

                '<circle class="donut-segment" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#70a8dc" stroke-width="5" stroke-dasharray="40 60" stroke-dashoffset="25"></circle>' +
                '<circle class="donut-segment" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#9cc1d7" stroke-width="5" stroke-dasharray="20 80" stroke-dashoffset="85"></circle>' +
                '<circle class="donut-segment" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#377bbc" stroke-width="5" stroke-dasharray="30 70" stroke-dashoffset="65"></circle>' +
              '</svg>' +
              '<div class="icon">&#xf187;</div>' +
            '</div>' +
            '<table></table>' +
          '</div>').appendTo(parentEl),

        tableEl = el.find('table'),

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
          // labelEl.css('width', 0.6 * percent + '%');
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

          tableEl.empty();
          buckets.slice(0, 3).forEach(function(bucket) {
            var pcnt = 100 * bucket.count / maxCount;
            tableEl.append(createBar(bucket, pcnt));
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
