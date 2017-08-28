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

  /** Fills the element with the first value in the array **/
  BaseCard.prototype.fillWithFirst = function(element, arr) {
    if (arr.length > 0) element.html(arr[0]);
  };

  BaseCard.prototype.renderIdentifiers = function(element, uris) {
    var identifiers = uris.map(function(uri) { return ItemUtils.parseEntityURI(uri); });
    identifiers.forEach(function(id) {
      var formatted = (id.shortcode) ? id.shortcode + ':' + id.id : id.uri,
          li = jQuery('<li><a href="' + id.uri + '" target="_blank">' + formatted + '</a></li>');

      if (id.color) li.css('background-color', id.color);
      element.append(li);
    });
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
            var p = jQuery('<p class="ref ' + ItemUtils.getItemType(all[0]).toLowerCase() + '"></p>'),

                /** Renders the 'N more results' link for a referenced item **/
                renderResultsAt = function(item) {
                  var identifiers = item.is_conflation_of[0].identifiers[0],

                      count = resultsAtReferenced.find(function(c) {
                        return identifiers.indexOf(c.identifier) > -1;
                      });

                  if (count && count.resultCount > 1) {
                    var more = count.resultCount - 1,
                        label = (more > 1) ? ' more results' : ' more result',
                        link = jQuery(
                          '<span class="more"> · ' +
                            '<a class="filter" href="#">' + Formatting.formatNumber(more) + label + '</a>' +
                          '<span>');

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
  BaseCard.prototype.renderInboundLinks = function(element, thisItem, count) {
    if (count > 0) {
      var ref = jQuery(
        '<span class="inbound-links">' +
          '<a class="local-search" href="#">' +
            Formatting.formatNumber(count) + ' items</a> link here' +
        '</span>');

      ref.find('a').data('at', thisItem);
      element.append(ref);
    } else {
      element.append('No items link here');
    }
  };

  return BaseCard;

});
