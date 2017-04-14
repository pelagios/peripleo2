define([
  'ui/common/hasEvents',
  'ui/controls/selection/cards/datasetCard',
  'ui/controls/selection/cards/objectCard',
  'ui/controls/selection/cards/personCard',
  'ui/controls/selection/cards/placeCard'
], function(
  HasEvents,
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
                (!visible && item) ? 'slideDown' : false, // Closed + select

              t = item.item_type;

          // Clear & set depicition in any case
          empty();
          setDepiction(item);

          // Then defer to the appropriate card implementation
          if (t.indexOf('PLACE') > -1) {
            new PlaceCard(cardEl, item);
          } else if (t.indexOf('OBJECT') > -1) {
            new ObjectCard(cardEl, item);
          } else if (t.indexOf('PERSON') > -1) {
            new PersonCard(cardEl, item);
          } else if (t.indexOf('DATASET') > -1) {
            new DatasetCard(cardEl, item);
          } else {
            // TODO implement future types
            console.log(item);
          }

          // Close/open as needed
          if (slideAction)
            element.velocity(slideAction, { duration: SLIDE_DURATION });
        };

    this.show = show;

    HasEvents.apply(this);
  };
  SelectionPanel.prototype = Object.create(HasEvents.prototype);

  return SelectionPanel;

});
