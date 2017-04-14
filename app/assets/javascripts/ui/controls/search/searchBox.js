define([
  'ui/common/hasEvents',
  'ui/controls/search/autocomplete'
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
         };

    searchBoxForm.submit(onSubmit);
    autocomplete.on('selectOption', onSelectOption);

    HasEvents.apply(this);
  };
  SearchBox.prototype = Object.create(HasEvents.prototype);

  return SearchBox;

});
