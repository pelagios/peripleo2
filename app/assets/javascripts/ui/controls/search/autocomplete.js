define(['ui/common/hasEvents'], function(HasEvents) {

  var Autocomplete = function(form, input) {
    input.typeahead({
      hint: true,
      highlight: true,
      minLength: 1
    },{
      async: true,
      limit: 100,
      display: function(obj) { return obj.text; },
      source: function(query, syncResults, asyncResults) {
        jQuery.getJSON('/api/suggest?q=' + query, function(results) {
          console.log(results);
          asyncResults(results);
        });
      }
    });

    HasEvents.apply(this);
  };
  Autocomplete.prototype = Object.create(HasEvents.prototype);

  return Autocomplete;

});
