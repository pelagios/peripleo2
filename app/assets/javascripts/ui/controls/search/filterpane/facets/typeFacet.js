define([
  'ui/common/formatting'
], function(Formatting) {

  var ICONS = {
        PLACE   : '&#xf041;',
        OBJECT  : '&#xf219;',
        PERSON  : '&#xf007;',
        DATASET : '&#xf187;'
      },

      MIN_PCNT = 29;

  var TypeFacet = function(parentEl) {

    var el = jQuery(
          '<div class="facet by-type left">' +
            '<div class="facet-aligner">' +
              '<ul></ul>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        template =
          '<li>' +
            '<div class="facet-bar"><span class="icon"></span><span class="label"></span></div>' +
          '</li>',

        list = el.find('ul'),

        update = function(arr) {
          var max = 0,

              buckets = arr.map(function(obj) {
                var name = Object.keys(obj)[0],
                    count = obj[name];

                if (count > max)
                  max = count;

                return { name: name, count: count };
              }),

              getWidth = function(val) {
                var pcnt = Math.round(100 * val / max);
                return Math.max(pcnt, MIN_PCNT) + '%';
              },

              render = function(bucket) {
                var li = jQuery(template),
                    icon = li.find('.icon'),
                    label = li.find('.label');

                li.addClass(bucket.name);
                li.css('width', getWidth(bucket.count));

                icon.html(ICONS[bucket.name]);

                label.html(Formatting.formatNumber(bucket.count));

                list.append(li);
              };

          list.empty();
          buckets.slice(0, 3).forEach(render);
        };

    this.update = update;
  };

  return TypeFacet;

});
