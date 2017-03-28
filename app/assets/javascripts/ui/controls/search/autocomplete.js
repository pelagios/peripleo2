define(['ui/common/hasEvents'], function(HasEvents) {

  var Autocomplete = function(form, input) {
    var self = this;

    input.typeahead({
      hint: false,
      highlight: false,
      minLength: 1
    },{
      async: true,
      limit: 42, // Behold: https://github.com/twitter/typeahead.js/issues/1312
      display: function(obj) { return obj.text; },
      templates: {
        suggestion: function(data) {
          var element = jQuery(
            '<div>' +
              '<span class="label">' + data.text + '</span>' +
            '</div>');

          if (data.item_type) {
            element.addClass('entity ' + data.item_type[0]);
            if (data.description)
              element.append('<span class="description">' + data.description + '</span>');
          } else {
            element.addClass('text');
          }

          return element;
        }
      },
      source: function(query, syncResults, asyncResults) {
        jQuery.getJSON('/api/suggest?q=' + query, function(results) {
          asyncResults(results);
        });
      }
    });

    input.on('typeahead:select', function(e, data) {
      self.fireEvent('selectOption', data);
    });

    HasEvents.apply(this);
  };
  Autocomplete.prototype = Object.create(HasEvents.prototype);

  return Autocomplete;

});
