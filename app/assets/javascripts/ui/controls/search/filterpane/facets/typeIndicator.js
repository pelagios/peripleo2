define([
  'ui/common/formatting',
  'ui/common/hasEvents'
], function(Formatting, HasEvents) {

      /**
       * Some (sub)facets are irrelevant and are not countent. E.g.
       * DATASET sub-types (AUTHORITY, AUTHORITY_GAZETTEER, etc.)
       */
  var RELEVANT_FACETS = [ 'OBJECT', 'PLACE', 'PERSON', 'PERIOD', 'DATASET' ],

      LABELS = {
        OBJECT  : [ 'object',  'objects'  ],
        PLACE   : [ 'place',   'places'   ],
        PERSON  : [ 'person',  'people'   ],
        PERIOD  : [ 'period',  'periods'  ],
        DATASET : [ 'dataset', 'datasets' ]
      },

      capitalize = function(str) {
        return str.charAt(0).toUpperCase() + str.substr(1);
      };

  var TypeIndicator = function(graphEl, countsEl) {

    var self = this,

        clickbuffer = jQuery('<div class="clickbuffer"></div>').appendTo(graphEl),

        bar = jQuery('<div class="bar"></div>').appendTo(graphEl),

        counts = jQuery('<ul></ul>').appendTo(countsEl),

        update = function(buckets) {

          var relevantBuckets = buckets.filter(function(b) {
                var type = b.path[0].id;
                return RELEVANT_FACETS.indexOf(type) > -1;
              }),

              updateBar = function() {
                var totalCount = relevantBuckets.reduce(function(acc, b) {
                      return acc + b.count;
                    }, 0);

                    addSegment = function(b) {
                      var minW = 1.5, // Make sure even small buckets remain visible
                          maxW = 100 - (relevantBuckets.length - 1) * minW,
                          w = 100 * b.count / totalCount,
                          normalized = Math.min(Math.max(w, minW), maxW);

                      bar.append('<span class="segment ' + b.path[0].id + '" style="width:' + normalized + '%"></span>');
                    };

                bar.empty();
                relevantBuckets.forEach(addSegment);
              },

              updateCounts = function() {
                var renderCount = function(bucket) {
                      var t = bucket.path[0].id,
                          c = bucket.count,
                          label = (c > 1) ? LABELS[t][1] : LABELS[t][0];

                      counts.append(
                        '<li class="col" data-type="' + t + '">' +
                          '<span class="value">' +
                            '<span class="count">' + Formatting.formatNumber(c) + '</span> ' + label +
                          '</span>' +
                        '</li>');
                    };

                counts.empty();
                relevantBuckets.forEach(renderCount);
              };

          updateBar();
          updateCounts();
        },

        /** Clicking on a count fires the corresponding type filter event **/
        onClickCount = function(e) {
          var li = jQuery(e.target).closest('li');
              type = li.data('type');

          self.fireEvent('setFilter', {
            filter: 'types',
            values: [{
              identifier: type,
              label: capitalize(LABELS[type][1])
            }]
          });
        },

        /** Click on the bar will notify the parent component, which handles the sliding pane **/
        onClickBar = function() {
          self.fireEvent('click');
        };

    counts.on('click', 'li', onClickCount);
    graphEl.click(onClickBar);

    this.update = update;

    HasEvents.apply(this);
  };
  TypeIndicator.prototype = Object.create(HasEvents.prototype);

  return TypeIndicator;

});
