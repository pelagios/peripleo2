define([], function() {

  var SearchPanel = function(container) {

    var element = jQuery(
          '<div id="searchpanel-container">' +
          '  <div class="searchbox">' +
          '    <form>' +
          '      <input class="search-input" type="text" name="q" autocomplete="off">' +
          '      <span class="search-input-icon icon">&#xf002;</span>' +
          '    </form>' +
          '  </div>' +
          '</div>'),

        /** DOM element shorthands **/
        searchForm = element.find('form'),
        searchInput = searchForm.find('input'),
        searchIcon = searchForm.find('.search-input.icon'),

        /** Updates the icon according to the contents of the search input field **/
        updateIcon = function() {
          var chars = searchInput.val().trim();

          if (chars.length === 0 && !isStateSubsearch) {
            searchIcon.html('&#xf002;');
            searchIcon.removeClass('clear');
          } else {
            searchIcon.html('&#xf00d;');
            searchIcon.addClass('clear');
          }
        },

        /** Handler for the 'X' clear button **/
        onResetSearch = function() {
          autoSuggest.clear();
          searchForm.submit();
          updateIcon();
        };

    searchForm.submit(function(e) {
      var chars = searchInput.val().trim();

      if (chars.length === 0)
        eventBroker.fireEvent(Events.SEARCH_CHANGED, { query : false });
      else
        eventBroker.fireEvent(Events.SEARCH_CHANGED, { query : chars });

      searchInput.blur();
      return false; // preventDefault + stopPropagation
    });

    // Append panel to the DOM
    container.append(element);
  };

  return SearchPanel;

});
