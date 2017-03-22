define([
  'ui/common/formatting',
  'ui/common/hasEvents'
], function(Formatting, HasEvents) {

  var SLIDE_DURATION = 120;

  var SelectionPanel = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="current-selection">' +
            '<div class="depiction"></div>' +
            '<div class="info">' +
              '<p class="in-dataset"></p>' +
              '<h3></h3>' +
              '<p class="homepage"></p>' +
              '<p class="temporal-bounds"></p>' +
            '</div>' +
            '<div class="references">' +
            '</div>' +
          '</div>').hide().appendTo(parentEl),

        depictionEl = element.find('.depiction'),

        titleEl = element.find('h3'),
        homepageEl = element.find('.homepage'),
        temporalBoundsEl = element.find('.temporal-bounds'),
        inDatasetEl = element.find('.in-dataset'),

        referencesEl = element.find('.references'),

        empty = function() {
          depictionEl.css('background-image', false);
          homepageEl.empty();
          temporalBoundsEl.empty();
          inDatasetEl.empty();
        },

        show = function(item) {

          // console.log(item);

          var visible = element.is(':visible'),

              slideAction = (visible && !item) ? 'slideUp' : // Open + deselect
                       (!visible && item) ? 'slideDown' : false, // Closed + select

              setDepiction = function() {
                if (item.depictions && item.depictions.length > 0) {
                  var firstURL = item.depictions[0].url;

                  // TODO pre-load image & report in case of 404
                  depictionEl.css('background-image', 'url(' + firstURL + ')');
                }
              },

              setInfo = function() {
                var title = (item.homepage) ?
                              '<a href="' + item.homepage + '" target="_blank">' + item.title + '</a>' :
                              item.title,

                    // TODO we'll do this pre-processing step on the server later!
                    datasetPath = function() {
                      var last = item.is_in_dataset[item.is_in_dataset.length - 1],
                          tuples = last.split('\u0007\u0007');

                      return tuples.map(function(str) {
                        var tuple = str.split('\u0007');
                        return { 'id': tuple[0], 'title': tuple[1] };
                      });
                    };

                titleEl.html(title);

                if (item.homepage)
                  homepageEl.html(item.homepage);

                if (item.temporal_bounds) {
                  if (item.temporal_bounds.from === item.temporal_bounds.to)
                    temporalBoundsEl.html(Formatting.formatYear(item.temporal_bounds.from));
                  else
                    temporalBoundsEl.html(Formatting.formatYear(item.temporal_bounds.from) +
                       ' - ' + Formatting.formatYear(item.temporal_bounds.to));
                }

                datasetPath().forEach(function(segment) {
                  inDatasetEl.append('<span><a href="#">' + segment.title + '</a></span>');
                });
              },

              setReferences = function() {
                // TODO load indicator
                // TODO there will be more than just findspots in the future!
                jsRoutes.controllers.api.ItemAPIController.getReferences(item.identifiers[0])
                  .ajax()
                  .done(function(response) {
                    var places = response.references.PLACE,
                        head = (places && places.length > 3) ? places.slice(0, 3) : places;

                    if (head) {
                      head.forEach(function(place) {
                        referencesEl.html(
                          '<p class="findspot">' +
                            '<span><a href="#" title="' + place.description + '">' + place.title + '</a></span>' +
                          '</p>');
                      });

                    }
                  });
              };

          empty();
          setDepiction();
          setInfo();
          setReferences();

          if (slideAction)
            element.velocity(slideAction, { duration: SLIDE_DURATION });
        };

    this.show = show;

    HasEvents.apply(this);
  };
  SelectionPanel.prototype = Object.create(HasEvents.prototype);

  return SelectionPanel;

});
