define([], function() {

  var URLBar = function() {

    var segments = {},

        /** Updates the URL field - now! **/
        updateNow = function() {
          var segment = jQuery.map(segments, function(val, key) {
            return key + '=' + val;
          });

          history.pushState(null, null, '#' + segment);
        },

        setQuery = function(query) {

        },

        updateFilters = function(diff) {

        },

        clearFilters = function() {

        },

        setTimerange = function(range) {

        },

        setSelection = function(item) {
          // if (item)
          //   segments.selected = encodeURIComponent(ItemUtils.getURIs(item)[0]);
          // else
          //   delete segments.selected;

          // updateNow();
        },

        setFilterpaneOpen = function(open) {

        };

    this.setQuery = setQuery;
    this.updateFilters = updateFilters;
    this.clearFilters = clearFilters;
    this.setTimerange = setTimerange;
    this.setSelection = setSelection;
    this.setFilterpaneOpen = setFilterpaneOpen;
  };

  return URLBar;

});
