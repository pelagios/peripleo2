define([], function() {

  var SIZE = {
        SMALL  : 3,
        MEDIUM : 4,
        LARGE  : 8
      },

      BASE_STYLE = {
        color       : '#a64a40',
        opacity     : 1,
        fillColor   : '#e75444',
        fillOpacity : 1,
        weight      : 1.5,
        radius      : SIZE.MEDIUM
      };

  return {

    POINT : {

      RED : (function() { return jQuery.extend({}, BASE_STYLE); })()

    }

  };

});
