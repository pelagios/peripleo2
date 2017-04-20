define(['ui/common/itemUtils'], function(ItemUtils) {

  var URLBar = function() {

    var segments = {},

        /** Updates the URL field - now! **/
        updateNow = function() {
          var segment = jQuery.map(segments, function(val, key) {
            return key + '=' + val;
          });

          history.pushState(null, null, '#' + segment.join('&'));
        },

        setQuery = function(query) {
          if (query)
            segments.q = query;
          else
            delete segments.q;
          updateNow();
        },

        updateFilters = function(diff) {
          // TODO
        },

        clearFilters = function() {
          // TODO
        },

        setTimerange = function(range) {
          if (range) {
            segments.from = range.from;
            segments.to = range.to;
          } else {
            delete segments.from;
            delete segments.to;
          }
          updateNow();
        },

        setSelection = function(item) {
          if (item)
            segments.selected = encodeURIComponent(ItemUtils.getURIs(item)[0]);
          else
            delete segments.selected;

          updateNow();
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
