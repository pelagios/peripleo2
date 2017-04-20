define([], function() {

  var URLBar = function() {

    var segments = {},

        /** Updates the URL field - now! **/
        updateNow = function() {
          var segment = jQuery.map(segments, function(val, key) {
            return key + '=' + val;
          });

          history.pushState(null, null, '#' + segment);
        };

        /*
        setSelection = function(item) {
          if (item)
            segments.selected = encodeURIComponent(ItemUtils.getURIs(item)[0]);
          else
            delete segments.selected;

          updateNow();
        },
        */

  };

  return URLBar;

});
