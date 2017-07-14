define([
  'ui/common/formatting',
  'ui/common/itemUtils',
  'ui/controls/selection/cards/baseCard',
  'ui/api'], function(Formatting, ItemUtils, BaseCard, API) {

  var ObjectCard  = function(parentEl, item, args) {

    var self = this,

        element = jQuery(
          '<div class="item-info">' +
            '<p class="item-is-in"></p>' +
            '<h3 class="item-title"></h3>' +
            '<p class="item-homepage"></p>' +
            '<p class="item-temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        inDataset  = element.find('.item-is-in'),
        title      = element.find('.item-title'),
        homepage   = element.find('.item-homepage'),
        tempBounds = element.find('.item-temporal-bounds'),

        snippetsContainer = jQuery(
          '<div class="snippets-outer"><div class="snippets-inner"></div></div>').hide().appendTo(parentEl),

        references = jQuery('<div class="item references"></div>').appendTo(parentEl),

        /** We're assuming, for now, that objects only have one record **/
        record = item.is_conflation_of[0],

        renderInfo = function() {
          var titleHtml = (record.homepage) ?
                '<a href="' + record.homepage + '" target="_blank">' + item.title + '</a>' :
                item.title;

          self.renderHierarchyPath(inDataset, record.is_in_dataset);
          self.fill(title, titleHtml);
          self.fillIfExists(homepage, record.homepage);
          self.fillTemporalBounds(tempBounds, item.temporal_bounds);
        },

        renderSnippets = function() {
          var snippetsInner = snippetsContainer.find('.snippets-inner'),

              // Identifier of this object
              identifier = record.identifiers[0],

              // Identifier of the item by which the references search should be restricted
              referencing = (args.selected_via) ?
                args.selected_via.is_conflation_of[0].identifiers[0] : false,

              // One reference (generally) consists of multiple snippets.
              // Each snippet is one query-match inside this reference's text context.
              renderSnippets = function(reference) {
                var element = jQuery('<div class="reference"><ul></ul></div>')
                      .appendTo(snippetsInner),
                    ul = element.find('ul');

                reference.snippets.forEach(function(s) {
                  ul.append('<li>' + s + '</li>');
                });

                if (reference.homepage)
                  element.append(Formatting.formatClickableURL(reference.homepage));
              };

          if (referencing)
            API.getReferences(identifier, referencing, args.query_phrase).done(function(results) {
              if (results.total > 0) {
                results.items.forEach(function(reference) {
                  renderSnippets(reference);
                });
                snippetsContainer.show();
              }
            });
        },

        renderReferenced = function() {
          var places = (args.selected_via) ? [ args.selected_via ] : args.referenced.PLACE,

              head = (places && places.length > 3) ? places.slice(0, 3) : places,

              people = args.referenced.PERSON;

          if (head) {
            head.forEach(function(place) {
              var identifiers = ItemUtils.getURIs(place),

                  counts = jQuery.grep(args.referenceCounts, function(r) {
                    return identifiers.indexOf(r.identifier) > -1;
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

                      el.find('a').data('referencing', place);
                      return el;
                    } else {
                      return false;
                    }
                  })(),

                  refEl = jQuery(
                    '<p class="ref place">' +
                      '<span class="title"><a href="#">' + place.title + '</a></span>' +
                    '</p>');

              if (moreResultsEl) // Append the 'more results link' if there is one
                refEl.append(moreResultsEl);

              references.append(refEl);
            });
          }

          if (people) {
            // TODO hack!
            people.forEach(function(p) {
              references.append(
                '<p class="ref person">' +
                  '<span class="title"><a href="#" class="destination" data-id="' + p.identifiers[0] + '">' + p.title + '</a></span>' +
                '</p>');
            });
          }
        };

    BaseCard.apply(this);

    renderInfo();
    renderSnippets();
    renderReferenced();
  };
  ObjectCard.prototype = Object.create(BaseCard.prototype);

  return ObjectCard;

});
