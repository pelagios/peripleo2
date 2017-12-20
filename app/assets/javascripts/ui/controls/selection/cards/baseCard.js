define([
  'ui/common/formatting',
  'ui/common/itemUtils'
], function(Formatting, ItemUtils) {

  /** Contains common shorhands used in the selection box cards **/
  var BaseCard = function() {};

  /** Fills the element with the given HTML content **/
  BaseCard.prototype.fill = function(element, html) {
    element.html(html);
  };

  /** Like above, but handles cases where html is falsy **/
  BaseCard.prototype.fillIfExists = function(element, html) {
    if (html) element.html(html);
  };

  /**
   * Fills the element with a 'preferred' description.
   *
   * Per convention (and for lack of something better) we'll pick the longest
   * English description, or longest description without a language tag, if any.
   */
  BaseCard.prototype.fillDescription = function(element, item) {
    var descriptionsWithSource = item.is_conflation_of.reduce(function(all, record) {
          if (record.descriptions)
            return all.concat(record.descriptions.map(function(d) {
                d.uri = record.uri;
                return d;
              }));
          else
            return all;
        }, []),

        sortedByLength = descriptionsWithSource.sort(function(a, b) {
          return b.description.length - a.description.length;
        }),

        longestPleiadesDescription = sortedByLength.find(function(d) {
          return d.uri.indexOf('pleiades.stoa.org') > -1;
        }),

        englishOnly = sortedByLength.filter(function(d) {
          return d.language === 'EN';
        }),

        // Top priority goes to longest Pleiades description, then longest English, then
        // longest any language
        topRanked = (longestPleiadesDescription) ? longestPleiadesDescription :
          (englishOnly.length > 0) ? englishOnly[0] :
            (sortedByLength.length > 0) ? sortedByLength[0] : false;

    if (topRanked)
      element.html(topRanked.description +
        ' <span class="source">' + Formatting.formatClickableURL(topRanked.uri) + '</span>');
    else
      element.hide();
  };

  BaseCard.prototype.renderIdentifiers = function(element, uris) {
    var groupByShortcode = function(arr) {
          return arr.reduce(function(grouped, identifier) {
            (grouped[identifier.shortcode] = grouped[identifier.shortcode] || []).push(identifier);
            return grouped;
          }, {});
        },

        toList = function(identifiers) {
          var grouped = groupByShortcode(identifiers),
              keys = Object.keys(grouped),
              firstOfEach = [],
              remainder = [];

          keys.forEach(function(key) {
            var group = grouped[key];

            firstOfEach.push(grouped[key][0]);
            if (group.length > 1)
              remainder = remainder.concat(group.slice(1));
          });

          return firstOfEach.concat(remainder);
        },

        identifiers = uris.sort().map(function(uri) { return ItemUtils.parseEntityURI(uri); }),

        firstThree = toList(identifiers).slice(0, 3),

        more = Math.max(0, identifiers.length - 3);

    firstThree.forEach(function(id) {
      var formatted = (id.shortcode) ? id.shortcode + ':' + id.id : id.uri,
          li = jQuery('<li><a href="' + id.uri + '" target="_blank">' + formatted + '</a></li>');

      if (id.color) li.css('background-color', id.color);
      element.append(li);
    });

    if (more > 0)
      element.append('<li class="more">' + more + ' more</li>');
  };

  /** Fills the element with the given temporalBounds value **/
  BaseCard.prototype.fillTemporalBounds = function(element, temporalBounds) {
    if (temporalBounds)
      element.html(Formatting.formatTemporalBounds(temporalBounds));
  };

  BaseCard.prototype.renderHierarchyPath = function(element, path) {
    if (path) {
      ItemUtils.getHierarchyPath(path).forEach(function(seg) {
        element.append(
          '<span>' +
            '<a class="destination" data-id="' + seg.id + '" href="#">' + seg.title + '</a>' +
          '</span>');
      });
    }
  };

  /**
   * Renders the list of items referenced by this item.
   *
   * Display will vary depending on whether this item was selected *via*
   * a specific item. I.e. a piece of literature selected via a place on the map
   * will display this place (the "via-place"), rather than the top places in the
   * document as a whole.
   * If the item was not selected via a referenced item (i.e. just via the result
   * list or from the autosuggest dropdown), the list will just display the top 3 referenced
   * items per type (place, person, period).
   */
  BaseCard.prototype.renderReferenced = function(element, referenced, resultsAtReferenced, selectedVia) {
    var typeOfVia = (selectedVia) ? ItemUtils.getItemType(selectedVia) : false,

        renderList = function(all) {
          if (all && all.length > 0) {
            var p = jQuery('<p class="refs ' + ItemUtils.getItemType(all[0]).toLowerCase() + '"></p>'),

                /** Renders the 'N more results' link for a referenced item **/
                renderResultsAt = function(item) {
                  var identifiers = item.is_conflation_of[0].identifiers[0],

                      count = resultsAtReferenced.find(function(c) {
                        return identifiers.indexOf(c.identifier) > -1;
                      });

                  if (count && count.resultCount > 1) {
                    var more = count.resultCount - 1,
                        link = jQuery(
                          '<span class="ref">' +
                            '<span class="more"> (' +
                              '<a class="filter" href="#">' + Formatting.formatNumber(more) + ' more</a>)' +
                            '<span>' +
                          '</span>');

                      link.find('a').data('referencing', item);
                      return link;
                  }
                },

                topThree = all.slice(0, 3).map(function(ref) {
                  var id = ref.is_conflation_of[0].identifiers[0],
                      resultsAt = renderResultsAt(ref);
                      span = jQuery(
                        '<span class="title">' +
                          '<a href="#" class="destination" data-id="' + id + '">' + ref.title + '</a>' +
                        '</span>');

                  if (resultsAt) return span.add(resultsAt);
                  else return span;
                }),

                renderHasMore = function() {
                  // TODO if there are more references, render a link
                };

            p.append(topThree);

            if (all.length > 3)
              p.append(renderHasMore());

            element.append(p);
          }
        };

    if (selectedVia) {
      // We'll show the 'via' item as first in list

      // TODO show 'and N others' label
      renderList([ selectedVia ]);
    } else {
      // No selection via entity - just render the list Places, People, Periods
      renderList(referenced.PLACE);
      renderList(referenced.PERSON);
      renderList(referenced.PERIOD);
    }
  };

  /** Renders the number of items that link to this item **/
  BaseCard.prototype.renderInboundLinks = function(element, count) {
    if (count > 0)
      element.append(
        '<span class="inbound-links">' +
          '<a class="local-search" href="#">' +
            Formatting.formatNumber(count) + ' items</a> link here' +
        '</span>');
    else
      element.append('No items link here');
  };

  return BaseCard;

});
