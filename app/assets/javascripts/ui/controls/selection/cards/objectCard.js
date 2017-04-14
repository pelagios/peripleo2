define(['ui/common/formatting', 'ui/api'], function(Formatting, API) {

  var ObjectCard  = function(parentEl, item) {
    var infoEl = jQuery(
          '<div class="info">' +
            '<p class="in-dataset"></p>' +
            '<h3></h3>' +
            '<p class="homepage"></p>' +
            '<p class="temporal-bounds"></p>' +
          '</div>').appendTo(parentEl),

        inDatasetEl  = infoEl.find('.in-dataset'),
        titleEl      = infoEl.find('h3'),
        homepageEl   = infoEl.find('.homepage'),
        tempBoundsEl = infoEl.find('.temporal-bounds'),

        referencesEl = jQuery(
          '<div class="references"></div>').appendTo(parentEl),

        // TODO we'll assume that objects only have one record for now
        record = item.is_conflation_of[0],

        renderInfo = function() {
          var title = (record.homepage) ?
                '<a href="' + record.homepage + '" target="_blank">' + item.title + '</a>' :
                item.title,

              // TODO we'll do this pre-processing step on the server later!
              datasetPath = function() {
                var last = record.is_in_dataset[record.is_in_dataset.length - 1],
                    tuples = last.split('\u0007\u0007');

                return tuples.map(function(str) {
                  var tuple = str.split('\u0007');
                  return { 'id': tuple[0], 'title': tuple[1] };
                });
              };

          titleEl.html(title);

          if (record.homepage)
            homepageEl.html(record.homepage);

          if (item.temporal_bounds) {
            if (item.temporal_bounds.from === item.temporal_bounds.to)
              tempBoundsEl.html(Formatting.formatYear(item.temporal_bounds.from));
            else
              tempBoundsEl.html(Formatting.formatYear(item.temporal_bounds.from) +
                 ' - ' + Formatting.formatYear(item.temporal_bounds.to));
          }

          datasetPath().forEach(function(segment) {
            inDatasetEl.append('<span><a href="#">' + segment.title + '</a></span>');
          });
        },

        renderReferences = function() {
          // TODO load indicator
          API.getReferences(record.uri).done(function(response) {
            var places = response.PLACE,
                head = (places && places.length > 3) ? places.slice(0, 3) : places;

            if (head) {
              head.forEach(function(place) {
                referencesEl.html(
                  '<p class="findspot">' + // TODO we will have more than just findspots in the future!
                    '<span><a href="#" title="' + place.description + '">' + place.title + '</a></span>' +
                  '</p>');
              });
            }
          });
        };

    renderInfo();
    renderReferences();
  };

  return ObjectCard;

});
