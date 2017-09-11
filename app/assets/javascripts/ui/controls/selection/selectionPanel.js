define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/controls/selection/cards/datasetCard',
  'ui/controls/selection/cards/objectCard',
  'ui/controls/selection/cards/periodCard',
  'ui/controls/selection/cards/personCard',
  'ui/controls/selection/cards/placeCard',
  'ui/controls/selection/depiction/iiifView',
  'ui/controls/selection/depiction/imageView'
], function(
  HasEvents,
  ItemUtils,
  DatasetCard,
  ObjectCard,
  PeriodCard,
  PersonCard,
  PlaceCard,
  IIIFView,
  ImageView
) {

  var SLIDE_OPTS = { duration: 120 };

  var SelectionPanel = function(parentEl) {

    var self = this,

        currentSelection = false,

        element = jQuery(
          '<div id="current-selection">' +
            '<div class="depiction"><span class="attribution"></span></div>' +
            '<div class="dogear"></div>' +
            '<div class="card"></div>' +
          '</div>').hide().appendTo(parentEl),

        depictionContainer = element.find('.depiction').hide(),
        dogear = element.find('.dogear'),
        card = element.find('.card'),

        depictionView = false,

        empty = function() {
          if (depictionView) depictionView.destroy();
          card.empty();
          currentSelection = false;
        },

        /** Sets or removes the depiction image, sliding the panel up or down as needed **/
        setDepiction = function(item) {
          var isPanelVisible = depictionContainer.is(':visible'),

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
              }, []),

              randomIdx, randomDepiction;

          if (depictions.length > 0) {
            randomIdx = Math.floor(Math.random() * depictions.length);
            randomDepiction = depictions[randomIdx];

            if (randomDepiction.iiif_uri)
              depictionView = new IIIFView(depictionContainer, randomDepiction);
            else
              depictionView = new ImageView(depictionContainer, randomDepiction);

            if (!isPanelVisible) depictionContainer.velocity('slideDown', SLIDE_OPTS);
          } else if (isPanelVisible) {
            depictionContainer.velocity('slideUp', SLIDE_OPTS);
            depictionView = false;
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

          currentSelection = item;

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
        onLocalSearch = function() {
          self.fireEvent('localSearch', currentSelection);
          return false;
        },

        onLinkedDataView = function() {
          self.fireEvent('linkedDataView', currentSelection);
          return false;
        };

    element.on('click', '.destination', onSelectDestination);
    element.on('click', '.filter', onSetFilter);
    element.on('click', '.local-search', onLocalSearch);
    element.on('click', '.ld-view-link', onLinkedDataView);

    this.show = show;
    this.hide = hide;

    HasEvents.apply(this);
  };
  SelectionPanel.prototype = Object.create(HasEvents.prototype);

  return SelectionPanel;

});
