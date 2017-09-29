define([
  'ui/common/hasEvents',
  'ui/common/itemUtils',
  'ui/state/urlBar'
], function(HasEvents, ItemUtils, URLBar) {

  var History = function() {

    var self = this,

        pushState = function(searchState, selection, uiState) {
          var state = { search: searchState, selection: selection, ui: uiState },
              hash = URLBar.toHash(state);

          window.history.pushState(state, null, '#' + hash);
        },

        onStateBack = function(e) {
          // Event happens when
          // - user clicks back button, in which case state is in the event
          // - user pastes an URL into the bar, in which case we'll parse
          var state = (e.originalEvent.state) ?
                e.originalEvent.state : URLBar.parseHash();

          self.fireEvent('changeState', state);
        };

    jQuery(window).bind('popstate', onStateBack);

    this.pushState = pushState;

    HasEvents.apply(this);
  };
  History.prototype = Object.create(HasEvents.prototype);

  return History;

});
