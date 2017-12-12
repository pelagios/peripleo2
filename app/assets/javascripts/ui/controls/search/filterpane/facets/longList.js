define([
  'ui/common/hasEvents',
  'ui/common/formatting'
], function(HasEvents, Formatting) {

  var LongList = function(buckets) {

    var element = jQuery(
          '<div class="clicktrap">' +
            '<div class="modal-wrapper facet-longlist-wrapper">' +
              '<div class="modal facet-longlist">' +
                '<div class="modal-header facet-longlist-header">' +
                  '<h2>...</h2>' + // TODO
                  '<button class="icon tonicons close">&#xe897;</button>' +
                '</div>' +
                '<div class="modal-body facet-longlist-body">' +
                  // TODO
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>').appendTo(document.body),

        createMeter = function() {

        },

        init = function() {

          var modal = element.find('.facet-longlist'),

              close = function() {
                element.remove();
              },

              renderRow = function(bucket) {
                console.log(bucket);

                /*
                var label = (val.label) ? val.label : val.value,
                tooltip = Formatting.formatNumber(val.count) + ' Results',
                percentage = 100 * val.count / maxCount,
                li = Formatting.createMeter(label, tooltip, percentage);

                li.addClass('selected');
                li.attr('data-value', val.value);
                li.prepend('<span class="icon selection-toggle">&#xf046;</span>');
                li.click(function() { toggle(li); });
                list.append(li);
                */
              };

          modal.draggable({ handle: element.find('.facet-longlist-header') });
          buckets.forEach(renderRow);
          element.find('.close').click(close);
        };

    init();

    HasEvents.apply(this);
  };
  LongList.prototype = Object.create(HasEvents.prototype);

  return LongList;

});
