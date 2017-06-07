define([
  'ui/common/hasEvents',
  'ui/controls/search/searchbox/autocomplete'
], function(HasEvents, Autocomplete) {

  var SearchBox = function(parentEl) {

    var self = this,

        element = jQuery(
           '<div id="searchbox">' +
           '  <form>' +
           '    <input type="text" name="q" autocomplete="off">' +
           '    <span class="icon">&#xf002;</span>' +
           '  </form>' +
           '</div>').appendTo(parentEl),

         searchBoxForm = element.find('form'),
         searchBoxInput = searchBoxForm.find('input'),
         searchBoxIcon = searchBoxForm.find('.icon'),

         autocomplete = new Autocomplete(searchBoxForm, searchBoxInput),

         onKeyup = function() {
           // As soon as anything was typed, swap the spyglass icon with the 'X'
           var chars = searchBoxInput.val().trim();
           if (chars)
             searchBoxIcon.html('&#xf1ce;');
           else
             searchBoxIcon.html('&#xf002;');
         },

         onIconClicked = function() {
           // Different behavior depending on icon state:
           // - spyglass -> submit
           // - X        -> clear search
           // - loading  -> noop

         },

         onSubmit = function() {
           var chars = searchBoxInput.val().trim();

           if (chars.length === 0)
             self.fireEvent('change', false);
           else
             self.fireEvent('change', chars);

           searchBoxInput.blur();
           return false;
         },

         onSelectOption = function(option) {
           if (option.identifier)
             self.fireEvent('selectSuggestOption', option.identifier);
           else
             onSubmit();
         },

         /** Sets the query string - does NOT fire a change event **/
         setQuery = function(query) {
           if (query) searchBoxInput.val(query);
           else searchBoxInput.val('');
         };

    searchBoxForm.keyup(onKeyup);
    searchBoxForm.submit(onSubmit);

    searchBoxIcon.click(onIconClicked);

    autocomplete.on('selectOption', onSelectOption);

    this.setQuery = setQuery;

    HasEvents.apply(this);
  };
  SearchBox.prototype = Object.create(HasEvents.prototype);

  return SearchBox;

});
