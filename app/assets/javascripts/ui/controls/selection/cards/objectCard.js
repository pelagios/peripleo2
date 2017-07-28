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
                    ul = element.find('ul'),

                    // TODO quick hack
                    snippets = (item.hit_on_reference) ?
                      reference.snippets : [ reference.context ];

                snippets.forEach(function(s) {
                  ul.append('<li>' + s + '</li>');
                });

                if (reference.homepage)
                  element.append(Formatting.formatClickableURL(reference.homepage));
              },

              request;

          if (referencing) {
            request = (item.hit_on_reference) ?
              API.getReferences(identifier, referencing, args.query_phrase) :
              API.getReferences(identifier, referencing);

            request.done(function(results) {
              if (results.total > 0) {
                results.items.forEach(function(reference) {
                  renderSnippets(reference);
                });
                snippetsContainer.show();
              }
            });
          }

        };

    BaseCard.apply(this);

    renderInfo();
    renderSnippets();

    self.renderReferenced(references, args.referenced, args.referenceCounts, args.selected_via);
  };
  ObjectCard.prototype = Object.create(BaseCard.prototype);

  return ObjectCard;

});
