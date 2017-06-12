define([
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/api'
], function(Formatting, ItemUtils, API) {

  var DatasetCard  = function(parentEl, dataset, args) {
    var infoEl = jQuery(
          '<div class="item-info">' +
            '<p class="item-is-in"></p>' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-homepage"></h3>' +
            '<p class="item-description"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        partOfEl      = infoEl.find('.item-is-in'),
        titleEl       = infoEl.find('.item-title'),
        homepageEl    = infoEl.find('.item-homepage'),
        descriptionEl = infoEl.find('.item-description'),
        tempBoundsEl  = infoEl.find('.item-temporal-bounds'),

        subsetsEl = jQuery(
          '<div class="dataset subsets">' +
            '<h4></h4>' +
            '<ul></ul>' +
          '</div>').appendTo(parentEl).hide(),

        subsetsLabelEl = subsetsEl.find('h4'),
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
          var descriptions = ItemUtils.getDescriptions(dataset),
              timeHistogram = args.aggregations.filter(function(agg) {
                return agg.name === 'by_time';
              })[0].buckets;

          if (record.is_part_of)
            ItemUtils.getHierarchyPath(record.is_part_of).forEach(function(segment) {
              partOfEl.append('<span>' +
                '<a class="destination" data-id="' + segment.id + '" href="#">' + segment.title +
                '</a></span>');
            });

          if (record.homepage) {
            titleEl.html(
              '<a href="' + record.homepage + '" target="_blank">' + dataset.title + '</a>');
            homepageEl.html(
              '<a href="' + record.homepage + '" target="_blank">' + record.homepage + '</a>');
          } else {
            titleEl.html(dataset.title);
          }

          if (descriptions.length > 0)
            descriptionEl.html(descriptions[0].description);

          // TODO what if there are no dates?
          tempBoundsEl.html(Formatting.formatTemporalBounds(getTemporalBounds(timeHistogram)));
        },

        renderSubsets = function(subsets) {
          subsetsLabelEl.html(subsets.length + ' subsets');
          subsets.forEach(function(subset) {
            subsetsListEl.append(subset.title);
          });
          // subsetsEl.show();
        },

        renderStats = function() {
          var totalItems = args.total,
              topPlaces = args.top_places,
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

    renderInfo();
    renderStats();

    API.getParts(record.identifiers[0]).done(function(parts) {
      if (parts.length > 0) renderSubsets(parts);
    });
  };

  return DatasetCard;

});
