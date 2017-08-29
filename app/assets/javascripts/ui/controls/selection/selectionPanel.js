define([
  'ui/common/formatting',
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/controls/selection/cards/datasetCard',
  'ui/controls/selection/cards/objectCard',
  'ui/controls/selection/cards/periodCard',
  'ui/controls/selection/cards/personCard',
  'ui/controls/selection/cards/placeCard'
], function(
  Formatting,
  HasEvents,
  ItemUtils,
  DatasetCard,
  ObjectCard,
  PeriodCard,
  PersonCard,
  PlaceCard
) {

  var SLIDE_OPTS = { duration: 120 };

  var SelectionPanel = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="current-selection">' +
            '<div class="dogear"></div>' +
            '<div class="depiction"><span class="attribution"></span></div>' +
            '<div class="card"></div>' +
          '</div>').hide().appendTo(parentEl),

        dogear         = element.find('.dogear'),
        depiction      = element.find('.depiction').hide(),
        imgAttribution = depiction.find('.attribution'),
        card           = element.find('.card'),

        empty = function() {
          depiction.css('background-image', 'none');
          card.empty();
        },

        /** Sets or removes the depiction image, sliding the panel up or down as needed **/
        setDepiction = function(item) {
          var isPanelVisible = depiction.is(':visible');

              // Essentially an item.is_conflation_of.flatMap(_.depictions)
              depictions = item.is_conflation_of.reduce(function(depictions, record) {
                if (record.depictions) {
                  return depictions.concat(record.depictions.map(function(d) {
                    d.source = record.uri;
                    return d;
                  }));
                } else {
                  return depictions;
                }
              }, []);

          if (depictions.length > 0) {
            // TODO pre-load image & report in case of 404
            depiction.css('background-image', 'url(' + depictions[0].url + ')');
            imgAttribution.html(Formatting.formatClickableURL(depictions[0].source));
            if (!isPanelVisible) depiction.velocity('slideDown', SLIDE_OPTS);
          } else if (isPanelVisible) {
            depiction.velocity('slideUp', SLIDE_OPTS);
          }
        },

        show = function(item, args) {
          var isVisible = element.is(':visible'),

              slideAction = (isVisible && !item) ? 'slideUp' : // Open + deselect
                (!isVisible && item) ? 'slideDown' : false, // Closed + select

              itemType = ItemUtils.getItemType(item);

          dogear.attr('class', 'dogear ' + itemType);
          card.attr('class', 'card ' + itemType);

          // Clear & set depiction
          empty();
          setDepiction(item);

          // Then defer to the appropriate card implementation
          switch(itemType) {
            case 'OBJECT':
              new ObjectCard(card, item, args);
              break;
            case 'PLACE':
              new PlaceCard(card, item, args);
              break;
            case 'PERSON':
              new PersonCard(card, item, args);
              break;
            case 'PERIOD':
              new PeriodCard(card, item, args);
              break;
            case 'DATASET':
              new DatasetCard(card, item, args);
              break;
            default:
              // Should never happen, unless we have different types in the future
              console.log(item);
          }

          // Close/open as needed
          if (slideAction) element.velocity(slideAction, SLIDE_OPTS);
        },

        hide = function() {
          if (element.is(':visible')) element.velocity('slideUp', SLIDE_OPTS);
        },

        /** User clicked a navigation link to select a different item **/
        onSelectDestination = function(e) {
          var link = jQuery(e.target),
              identifier = link.data('id');
          self.fireEvent('select', identifier);
          return false;
        },

        /** The user clicked a filter link to filter by referenced item **/
        onSetFilter = function(e) {
          var link = jQuery(e.target),
              referencing = link.data('referencing'),

              filter = {
                filter : 'referencing',
                values : [{
                  identifier: referencing.is_conflation_of[0].identifiers[0],
                  label: referencing.title,
                  type: ItemUtils.getItemType(referencing)
                }]
              };

          self.fireEvent('setFilter', filter);
          return false;
        },

        /**
         * The user clicked a local search link to switch into a search for EVERYTHING
         * linked to this item.
         */
        onLocalSearch = function(e) {
          var link = jQuery(e.target),
              place = link.data('at');
          self.fireEvent('localSearch', place);
          return false;
        };

    element.on('click', '.destination', onSelectDestination);
    element.on('click', '.filter', onSetFilter);
    element.on('click', '.local-search', onLocalSearch);

    this.show = show;
    this.hide = hide;

    HasEvents.apply(this);
  };
  SelectionPanel.prototype = Object.create(HasEvents.prototype);

  return SelectionPanel;

});
