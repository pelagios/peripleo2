define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/controls/selection/cards/datasetCard',
  'ui/controls/selection/cards/objectCard',
  'ui/controls/selection/cards/personCard',
  'ui/controls/selection/cards/placeCard'
], function(
  HasEvents,
  ItemUtils,
  DatasetCard,
  ObjectCard,
  PersonCard,
  PlaceCard
) {

  var SLIDE_DURATION = 120;

  var SelectionPanel = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="current-selection">' +
            '<div class="depiction"></div>' +
            '<div class="card"></div>' +
          '</div>').hide().appendTo(parentEl),

        depictionEl = element.find('.depiction').hide(),
        cardEl = element.find('.card'),

        empty = function() {
          depictionEl.css('background-image', false);
          cardEl.empty();
        },

        setDepiction = function(item) {
          var isPanelVisible = depictionEl.is(':visible');

              depictions = item.is_conflation_of.reduce(function(depictions, record) {
                if (record.depictions) return depictions.concat(record.depictions);
                else return depictions;
              }, []);

          if (depictions.length > 0) {
            // TODO pre-load image & report in case of 404
            depictionEl.css('background-image', 'url(' + depictions[0].url + ')');
            if (!isPanelVisible)
              depictionEl.velocity('slideDown', { duration: SLIDE_DURATION });
          } else if (isPanelVisible) {
            depictionEl.velocity('slideUp', { duration: SLIDE_DURATION });
          }
        },

        show = function(item, args) {
          var visible = element.is(':visible'),

              slideAction = (visible && !item) ? 'slideUp' : // Open + deselect
                (!visible && item) ? 'slideDown' : false; // Closed + select

          // Clear & set depicition in any case
          empty();
          setDepiction(item);

          // Then defer to the appropriate card implementation
          switch(ItemUtils.getItemType(item)) {
            case 'PLACE':
              new PlaceCard(cardEl, item, args);
              break;
            case 'OBJECT':
              new ObjectCard(cardEl, item, args);
              break;
            case 'PERSON':
              new PersonCard(cardEl, item, args);
              break;
            case 'DATASET':
              new DatasetCard(cardEl, item, args);
              break;
            default:
              // TODO implement future types
              console.log(item);
          }

          // Close/open as needed
          if (slideAction)
            element.velocity(slideAction, { duration: SLIDE_DURATION });
        },

        hide = function() {
          if (element.is(':visible'))
            element.velocity('slideUp', { duration: SLIDE_DURATION });
        },

        /** User clicked a direct link to a different item **/
        onSelectDestination = function(e) {
          var link = jQuery(e.target),
              identifier = link.data('id');
          self.fireEvent('select', identifier);
          return false;
        },

        /** User clicked a referenced item, in order to trigger a filtered search **/
        onSelectReference = function(e) {
          var link = jQuery(e.target),
              reference = link.data('reference');
          self.fireEvent('filterBy', reference);
          return false;
        };

    element.on('click', '.destination', onSelectDestination);
    element.on('click', '.filter', onSelectReference);

    this.show = show;
    this.hide = hide;

    HasEvents.apply(this);
  };
  SelectionPanel.prototype = Object.create(HasEvents.prototype);

  return SelectionPanel;

});
