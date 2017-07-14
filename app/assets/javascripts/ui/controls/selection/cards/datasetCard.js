define([
  'ui/common/aggregationUtils',
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/controls/selection/cards/baseCard',
  'ui/api'
], function(AggregationUtils, Formatting, ItemUtils, BaseCard, API) {

  var DatasetCard  = function(parentEl, dataset, args) {

    var self = this,

        element = jQuery(
          '<div class="item-info">' +
            '<p class="item-is-in"></p>' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-homepage"></h3>' +
            '<p class="item-description"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        partOf      = element.find('.item-is-in'),
        title       = element.find('.item-title'),
        homepage    = element.find('.item-homepage'),
        description = element.find('.item-description'),
        tempBounds  = element.find('.item-temporal-bounds'),

        subsets = jQuery('<div class="dataset subsets"><ul></ul></div>')
          .appendTo(parentEl).hide(),

        stats = jQuery('<div class="item references"></div>').appendTo(parentEl),

        /** It's safe to assume that datasets will have exactly one record **/
        record = dataset.is_conflation_of[0],

        renderInfo = function() {
          var timeHistogram = args.aggregations.filter(function(agg) {
                return agg.name === 'by_time';
              })[0].buckets,

              temporalBounds = (function() {
                var first = timeHistogram[0],
                    last = timeHistogram[timeHistogram.length - 1],
                    getYear = function(obj) {
                      return parseInt(Object.keys(obj)[0]);
                    };

                return { from: getYear(first), to: getYear(last) };
              })();

          self.renderHierarchyPath(partOf, record.is_part_of);

          if (record.homepage) {
            self.fill(title, '<a href="' + record.homepage + '" target="_blank">' + dataset.title + '</a>');
            self.fill(homepage, '<a href="' + record.homepage + '" target="_blank">' + record.homepage + '</a>');
          } else {
            self.fill(title, dataset.title);
          }

          self.fillWithFirst(description,
            ItemUtils.getDescriptions(dataset).map(function(d) { return d.description; }));
          self.fillTemporalBounds(tempBounds, temporalBounds);
        },

        renderStats = function() {
          var totalItems = args.total,
              topPlaces = args.top_referenced.PLACE,
              topPlacesCount = (topPlaces.length < 500) ? topPlaces.length: '500+';

          stats.append(
            '<span class="stats-items">' +
              '<span class="icon">&#xf219</span>' +
              '<span class="count">' +
                Formatting.formatNumber(totalItems) +
              '</span> items' +
            '</span><span class="sep"></span>' +

            '<span class="stats-links">' +
              '<span class="icon">&#xf0c1;</span> linked to ' +
              '<span class="count">' +
                topPlacesCount +
              '</span> places' +
            '</span>');
        },

        /** Fetches subset info via the API and inserts the info into the card **/
        renderSubsetsAsync = function() {
          var list = subsets.find('ul'),

              counts = AggregationUtils.getAggregation(args.aggregations, 'by_dataset'),

              render = function(parts) {
                parts.items.slice(0, 5).forEach(function(part) {
                  var id = part.is_conflation_of[0].identifiers[0];
                  list.append(
                    '<li>' +
                      '<a class="destination" data-id="' + id + '" href="#">' + part.title + '</a>' +
                      '<span class="count">(' +
                        Formatting.formatNumber(AggregationUtils.getCountForId(counts, id)) +
                      ' items)<span>' +
                    '</li>');
                });

                if (parts.items.length > 5)
                  subsets.append('<span class="more-subsets">+ <a href="#">' + (parts.items.length - 5) + ' more</a></span>');

                subsets.show();
              };

          API.getParts(record.identifiers[0]).done(function(parts) {
            if (parts.total > 0)
              render(parts);
          });
        };

    BaseCard.apply(this);

    renderInfo();
    renderSubsetsAsync();
    renderStats();
  };
  DatasetCard.prototype = Object.create(BaseCard.prototype);

  return DatasetCard;

});
