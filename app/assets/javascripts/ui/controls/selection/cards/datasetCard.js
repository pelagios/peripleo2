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

        subsetsEl = jQuery(
          '<div class="dataset subsets"><ul></ul></div>').appendTo(parentEl).hide(),

        subsetsListEl = subsetsEl.find('ul'),

        statsEl = jQuery(
          '<div class="item references"></div>').appendTo(parentEl),

        // Safe to assume that datasets only have one record
        record = dataset.is_conflation_of[0],

        getTemporalBounds = function(histogram) {
          var first = histogram[0],
              last = histogram[ histogram.length - 1 ],
              getYear = function(obj) {
                return parseInt(Object.keys(obj)[0]);
              };

          return { from: getYear(first), to: getYear(last) };
        },

        renderInfo = function() {
          var timeHistogram = args.aggregations.filter(function(agg) {
                return agg.name === 'by_time';
              })[0].buckets;

          self.renderHierarchyPath(partOf, record.is_part_of);

          if (record.homepage) {
            self.fill(title, '<a href="' + record.homepage + '" target="_blank">' + dataset.title + '</a>');
            self.fill(homepage, '<a href="' + record.homepage + '" target="_blank">' + record.homepage + '</a>');
          } else {
            self.fill(title, dataset.title);
          }

          self.fillWithFirst(description,
            ItemUtils.getDescriptions(place).map(function(d) { return d.description; }));

          // TODO what if there are no dates?
          tempBoundsEl.html(Formatting.formatTemporalBounds(getTemporalBounds(timeHistogram)));
        },

        renderSubsets = function(subsets) {
          var counts = AggregationUtils.getAggregation(args.aggregations, 'by_dataset');

          subsets.items.slice(0, 5).forEach(function(subset) {
            var id = subset.is_conflation_of[0].identifiers[0];
            subsetsListEl.append(
              '<li>' +
                '<a class="destination" data-id="' + id + '" href="#">' + subset.title + '</a>' +
                '<span class="count">(' +
                  Formatting.formatNumber(AggregationUtils.getCountForId(counts, id)) +
                ' items)<span>' +
              '</li>');
          });

          // TODO make this link do something
          if (subsets.items.length > 5)
            subsetsEl.append(
              '<span class="more-subsets">+ <a href="#">' + (subsets.items.length - 5) + ' more</a></span>');

          subsetsEl.show();
        },

        renderStats = function() {
          var totalItems = args.total,
              topPlaces = args.top_referenced.PLACE,
              topPlacesCount = (topPlaces.length < 500) ? topPlaces.length: '500+';

          statsEl.append(
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
        };

    BaseCard.apply(this);

    renderInfo();
    renderStats();

    API.getParts(record.identifiers[0]).done(function(parts) {
      if (parts.total > 0) renderSubsets(parts);
    });
  };
  DatasetCard.prototype = Object.create(BaseCard.prototype);

  return DatasetCard;

});
