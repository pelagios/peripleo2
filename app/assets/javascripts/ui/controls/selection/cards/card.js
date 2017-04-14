define(['ui/common/formatting'], function(Formatting) {

  var Card  = function() {

        /**
         * Helper to map the list of conflated records to a list values of the given
         * record property. (E.g. go from list of records to list of descriptions.)
         */
    var mapConflated = function(key) {
          return function(item) {
            var mapped = [];
            item.is_conflation_of.map(function(record) {
              var values = record[key];
              if (values)
                if (jQuery.isArray(values))
                  mapped = mapped.concat(values);
                else
                  mapped.push(values);
            });

            return mapped;
          };
        },

        /** We'll do this pre-processing step on the server later! **/
        getDatasetPath = function(record) {
          var last = record.is_in_dataset[record.is_in_dataset.length - 1],
              tuples = last.split('\u0007\u0007');

          return tuples.map(function(str) {
            var tuple = str.split('\u0007');
            return { 'id': tuple[0], 'title': tuple[1] };
          });
        },

        formatTemporalBounds = function(bounds) {
          if (bounds.from === bounds.to)
            return Formatting.formatYear(bounds.from);
          else
            return Formatting.formatYear(bounds.from) + ' - ' + Formatting.formatYear(bounds.to);
        };

    this.getDatasetPath = getDatasetPath;
    this.formatTemporalBounds = formatTemporalBounds;
    this.getURIs = mapConflated('uri');
    this.getDescriptions = mapConflated('descriptions');
    this.getNames = mapConflated('names');
  };

  return Card;

});
