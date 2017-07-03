define([
  'ui/common/hasEvents',
  'ui/common/formatting'
], function(HasEvents, Formatting) {

  var SLIDE_DURATION = 240,

      SEGMENT_COLORS = [ '#70a8dc', '#9cc1d7', '#377bbc' ],

      ROW_TEMPLATE = '<tr><td class="count"></td><td class="label"></td></tr>',

      /** Shorthand to fetch labels from a path **/
      toLabel = function(path) {
        return path.map(function(segment) {
          return '<span class="seg">' + segment.label + '</span>';
        }).join(' ');
      };

  var FacetChart = function(parentEl) {

    var self = this,

        el = jQuery(
          '<div class="chart-container">' +
            '<div class="donut">' +
              '<svg width="100%" height="100%" viewBox="0 0 42 42" class="donut">' +
                '<circle class="donut-hole" cx="21" cy="21" r="15.91549430918954" fill="#f5f7f7"></circle>' +
                '<circle class="donut-ring" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#d8d9db" stroke-width="5"></circle>' +
              '</svg>' +
              '<div class="icon">&#xf187;</div>' +
            '</div>' +
            '<table></table>' +
          '</div>').appendTo(parentEl),

        tableEl = el.find('table'),

        /**
         * Approach taken from
         * https://medium.com/@heyoka/scratch-made-svg-donut-pie-charts-in-html5-2c587e935d72
         */
        renderDonut = function(percentages) {
          var svg = el.find('svg')[0];

          el.find('.donut-segment').remove();
          percentages.reduce(function(offset, pcnt, idx) {
            var color = SEGMENT_COLORS[idx];

                // SVG is namespaced, so we can't just use jQuery
                circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');

            circle.setAttribute('class', 'donut-segment');
            circle.setAttribute('cx', 21);
            circle.setAttribute('cy', 21);
            circle.setAttribute('r', 15.91549430918954);
            circle.setAttribute('fill', 'transparent');
            circle.setAttribute('stroke', color);
            circle.setAttribute('stroke-width', 5);
            circle.setAttribute('stroke-dasharray', pcnt + ' ' + (100 - pcnt));
            circle.setAttribute('stroke-dashoffset', offset);

            svg.appendChild(circle);

            offset -= pcnt;
            if (offset < 0) offset += 100;
            return offset;
          }, 25); // Initial offset 25% counter-clockwise = 12 o'clock position
        },

        renderTable = function(buckets) {
          var renderRow = function(bucket) {
                var el = jQuery(ROW_TEMPLATE);

                el.data('path', bucket.path);
                el.find('.count').html(Formatting.formatNumber(bucket.count));
                el.find('.label').html(toLabel(bucket.path));

                return el;
              };

          tableEl.empty();

          // Only show top three buckets
          buckets.slice(0, 3).forEach(function(bucket) {
            tableEl.append(renderRow(bucket));
          });

          if (buckets.length > 3)
            tableEl.append(
              '<tr>' +
                '<td></td>' +
                '<td class="more">+ ' +
                  '<span class="label">' + (buckets.length - 3) + ' more</span>' +
                '</td>' +
              '</tr>');
        },

        update = function(buckets) {
          var totalCount = buckets.reduce(function(total, bucket) {
                return total + bucket.count;
              }, 0),

              percentages = buckets.slice(0, 3).map(function(bucket) {
                return Math.round(100 * bucket.count / totalCount);
              });

          renderTable(buckets);
          renderDonut(percentages);
        },

        toggle = function() {
          if (parentEl.is(':visible'))
            parentEl.velocity('slideUp', { duration: SLIDE_DURATION });
          else
            parentEl.velocity('slideDown', { duration: SLIDE_DURATION });
        },

        onSetFilter = function(e) {
          var li = jQuery(e.target).closest('tr'),
              path = li.data('path');

          self.fireEvent('setFilter', {
            filter: 'datasets', // TODO just a quick hack
            values: [{
              identifier: path[path.length - 1].id,
              label: toLabel(path)
            }]
          });
        };

    parentEl.on('click', 'tr', onSetFilter);

    this.toggle = toggle;
    this.update = update;

    HasEvents.apply(this);
  };
  FacetChart.prototype = Object.create(HasEvents.prototype);

  return FacetChart;

});
