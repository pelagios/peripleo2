define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/state/urlBar'
], function(HasEvents, ItemUtils, URLBar) {

  var History = function() {

    var self = this,

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