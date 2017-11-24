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
                var minW = 1.5, // Make sure even small buckets remain visible, 1.5% minimum

                    totalCount = relevantBuckets.reduce(function(acc, b) {
                      return acc + b.count;
                    }, 0),

                    // Note that the sum of all segment widths may be > 100% at this point!
                    segments = relevantBuckets.map(function(b) {
                      var w = 100 * b.count / totalCount,
                          width = Math.max(w, minW);

                      return { id: b.path[0].id, width: width };
                    }),

                    totalWidth = segments.reduce(function(acc, s) {
                      return acc + s.width;
                    }, 0),

                    segmentsNormalized = (totalWidth === 100) ? segments :
                      segments.map(function(s) {
                        return { id: s.id, width: s.width * 100 / totalWidth };
                      }),

                    addSegment = function(s) {
                      bar.append('<span class="segment ' + s.id + '" style="width:' + s.width + '%"></span>');
                    };

                bar.empty();
                segmentsNormalized.forEach(addSegment);
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
