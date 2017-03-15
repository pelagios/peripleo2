define(['ui/common/hasEvents'], function(HasEvents) {

  var SearchPanel = function(container) {

    var self = this,

       element = jQuery(
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

        onSubmit = function() {
          var chars = searchInput.val().trim();

          if (chars.length === 0)
            self.fireEvent('queryChange', false);
          else
            self.fireEvent('queryChange', chars);

          searchInput.blur();
          return false;
        },

        /** Handler for the 'X' clear button **/
        onResetSearch = function() {
          autoSuggest.clear();
          searchForm.submit();
          updateIcon();
        };

    searchForm.submit(onSubmit);
    container.append(element);

    HasEvents.apply(this);
  };
  SearchPanel.prototype = Object.create(HasEvents.prototype);

  return SearchPanel;

});
