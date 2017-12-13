define([
  'ui/common/hasEvents',
  'ui/common/formatting'
], function(HasEvents, Formatting) {

  var ROW_TEMPLATE =
        '<div class="meter">' +
          '<div class="bar"></div>' +
          '<div class="label"></div>' +
        '</div>';

  var LongList = function(dimension, buckets) {

    var self = this,

        element = jQuery(
          '<div class="clicktrap">' +
            '<div class="modal-wrapper facet-longlist-wrapper">' +
              '<div class="modal facet-longlist">' +
                '<div class="modal-header facet-longlist-header">' +
                  '<h2>' + buckets.length +' ' + dimension + '</h2>' +
                  '<button class="icon tonicons close">&#xe897;</button>' +
                '</div>' +
                '<div class="modal-body facet-longlist-body">' +
                  '<ul></ul>' +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>').appendTo(document.body),

        list = element.find('ul'),

        maxCount = buckets[0].count,

        init = function() {

          var close = function() {
                element.remove();
              },

              renderRow = function(bucket) {
                var row = jQuery(ROW_TEMPLATE),

                    bar = row.find('.bar'),

                    label = row.find('.label'),

                    // Make sure
                    pcnt = Math.max(60  * bucket.count / maxCount, 3),

                    onClick = function() {
                      self.fireEvent('setFilter', bucket.path);
                      close();
                    };

                bar.css('width', pcnt + '%');
                bar.attr('title', Formatting.formatNumber(bucket.count) + ' results');
                label.html(Formatting.formatPath(bucket.path));

                row.click(onClick);

                list.append(row);
              };

          element.find('.facet-longlist').draggable({
            handle: element.find('.facet-longlist-header')
          });

          buckets.forEach(renderRow);
          element.find('.close').click(close);
        };

    init();

    HasEvents.apply(this);
  };
  LongList.prototype = Object.create(HasEvents.prototype);

  return LongList;

});
