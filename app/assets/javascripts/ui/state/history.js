define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/state/urlBar'
], function(HasEvents, ItemUtils, URLBar) {

  var History = function() {

    var self = this,

        parseURLHash = function() {
          var hash = window.location.hash,
              keysVals = (hash.indexOf('#') === 0) ? hash.substring(1).split('&') : false,
              segments = {};

          if (keysVals) {
            keysVals.forEach(function(keyVal) {
              var asArray = keyVal.split('='),
                  key = asArray[0],
                  value = asArray[1];

              segments[key] = value;
            });

            return {
              search: {},
              ui: {

              }
            };
          }
        },

        pushState = function(searchState, uiState) {
          var state = { search: searchState, ui: uiState },
              hash = URLBar.toHash(state);

          window.history.pushState(state, null, '#' + hash);
        },

        onStateBack = function(e) {
          self.fireEvent('changeState', e.originalEvent.state);
        };

    jQuery(window).bind('popstate', onStateBack);

    this.pushState = pushState;

    HasEvents.apply(this);
  };
  History.prototype = Object.create(HasEvents.prototype);

  return History;

});
