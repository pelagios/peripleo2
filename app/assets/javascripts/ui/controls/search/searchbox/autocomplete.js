define([
  'ui/common/hasEvents',
  'ui/api'
], function(HasEvents, API) {

  var Autocomplete = function(form, input) {

    var self = this;

    // Typeahead setup
    input.typeahead({
      hint      : false,
      highlight : false,
      minLength : 1
    },{
      async     : true,
      limit     : 42, // Behold: https://github.com/twitter/typeahead.js/issues/1312
      display   : function(obj) { return obj.text; },

      templates : {

        suggestion: function(data) {
          var element = jQuery(
                '<div>' +
                  '<div class="decoration"></div>' +
                  '<div class="inner">' +
                    '<span class="label">' + data.text + '</span>' +
                  '</div>' +
                '</div>'),

              inner = element.find('.inner');

          if (data.item_type) {
            element.addClass('entity ' + data.item_type[0]);
            if (data.description)
              inner.append('<span class="description">' + data.description + '</span>');
          } else {
            element.addClass('text');
          }

          return element;
        }

      },

      source : function(query, syncResults, asyncResults) {
        API.suggest(query).done(function(results) {
          asyncResults(results);
        }).fail(function(error) {
          console.log(error);
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
