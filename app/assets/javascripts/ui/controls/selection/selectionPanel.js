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

        show = function(item) {
          var visible = element.is(':visible'),

              slideAction = (visible && !item) ? 'slideUp' : // Open + deselect
                (!visible && item) ? 'slideDown' : false; // Closed + select

          // Clear & set depicition in any case
          empty();
          setDepiction(item);

          // Then defer to the appropriate card implementation
          switch(ItemUtils.getItemType(item)) {
            case 'PLACE':
              new PlaceCard(cardEl, item);
              break;
            case 'OBJECT':
              new ObjectCard(cardEl, item);
              break;
            case 'PERSON':
              new PersonCard(cardEl, item);
              break;
            case 'DATASET':
              new DatasetCard(cardEl, item);
              break;
            default:
              // TODO implement future types
              console.log(item);
          }

          // Close/open as needed
          if (slideAction)
            element.velocity(slideAction, { duration: SLIDE_DURATION });
        },

        onSelect = function(e) {
          var link = jQuery(e.target),
              identifier = link.data('id');

          self.fireEvent('select', identifier);
          return false;
        };

    element.on('click', '.destination', onSelect);

    this.show = show;

    HasEvents.apply(this);
  };
  SelectionPanel.prototype = Object.create(HasEvents.prototype);

  return SelectionPanel;

});
