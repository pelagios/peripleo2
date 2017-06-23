define([
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/api'], function(Formatting, ItemUtils, API) {

  var ObjectCard  = function(parentEl, item, args) {

    var infoEl = jQuery(
          '<div class="item-info">' +
            '<p class="item-is-in"></p>' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-homepage"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        inDatasetEl  = infoEl.find('.item-is-in'),
        titleEl      = infoEl.find('.item-title'),
        homepageEl   = infoEl.find('.item-homepage'),
        tempBoundsEl = infoEl.find('.item-temporal-bounds'),

        snippetsEl = jQuery(
          '<div class="item snippets"></div>').hide().appendTo(parentEl),

        referencesEl = jQuery(
          '<div class="item references"></div>').appendTo(parentEl),

        // TODO we'll assume that objects only have one record for now
        record = item.is_conflation_of[0],

        renderInfo = function() {
          var title = (record.homepage) ?
                '<a href="' + record.homepage + '" target="_blank">' + item.title + '</a>' :
                item.title;

          ItemUtils.getHierarchyPath(record.is_in_dataset).forEach(function(segment) {
            inDatasetEl.append(
              '<span><a class="destination" data-id="' + segment.id + '" href="#">' +
                segment.title +
              '</a></span>');
          });

          titleEl.html(title);
          if (record.homepage)
            homepageEl.html(record.homepage);
          if (item.temporal_bounds)
            tempBoundsEl.html(Formatting.formatTemporalBounds(item.temporal_bounds));
        },

        renderSnippets = function() {
          var itemId = item.is_conflation_of[0].identifiers[0],
              refId = (args.selected_via) ?
                args.selected_via.is_conflation_of[0].identifiers[0] : false,

              renderSnippet = function(context) {
                var terms = args.query_phrase.split(' ');
                    snippet = context.trim();

                terms.forEach(function(term) {
                  snippet = snippet.replace(term, '<em>' + term + '</em>');
                });

                snippetsEl.append('<div class="snippet">...' + snippet + '...</div>');
              };

          if (refId)
            API.getSnippets(itemId, refId, args.query_phrase).done(function(result) {
              // TODO animate
              snippetsEl.show();
              if (result.total > 0)
                result.items.forEach(function(ref) {
                  renderSnippet(ref.context);
                });
            });
        },

        renderReferences = function() {
          var places = args.references.PLACE,
              head = (places && places.length > 3) ? places.slice(0, 3) : places;

          if (args.selected_via) {
            // TODO quick hack
            referencesEl.append(
              '<p class="ref-place">' +
                '<span class="title"><a href="#">' + args.selected_via.title + '</a></span>' +
              '</p>');
          } else if (head) {
            head.forEach(function(place) {
              var counts = jQuery.grep(args.resultCounts, function(r) {
                    return place.identifiers.indexOf(r.identifier) > -1;
                  }),

                  moreResultsEl = (function() {
                    // Show 'more results' link of there's at least 1 more result
                    if (counts.length > 0 && counts[0].resultCount > 1) {
                      var moreCount = counts[0].resultCount - 1,
                          moreLabel = (moreCount > 1) ? ' more results' : ' more result',

                          el = jQuery(
                            '<span class="more"> · ' +
                              '<a class="filter" href="#">' + Formatting.formatNumber(moreCount) +
                                moreLabel + '</a>' +
                            '<span>');

                      el.find('a').data('reference', place);
                      return el;
                    } else {
                      return false;
                    }
                  })(),

                  refEl = jQuery(
                    '<p class="ref-place">' +
                      '<span class="title"><a href="#">' + place.title + '</a></span>' +
                    '</p>');

              if (moreResultsEl) // Append the 'more results link' if there is one
                refEl.append(moreResultsEl);

              referencesEl.append(refEl);
            });
          }
        };

    renderInfo();
    renderSnippets();
    renderReferences();
  };

  return ObjectCard;

});
