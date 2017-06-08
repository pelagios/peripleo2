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
           '    <span class="icon search"></span>' +
           '  </form>' +
           '</div>').appendTo(parentEl),

         searchBoxForm = element.find('form'),
         searchBoxInput = searchBoxForm.find('input'),
         searchBoxIcon = searchBoxForm.find('.icon'),

         autocomplete = new Autocomplete(searchBoxForm, searchBoxInput),

         currentIconClass = 'icon search',

         onIconClicked = function() {
           if (searchBoxIcon.hasClass('search')) {
             // Click on spyglass triggers search
             onSubmit();
           } else if (searchBoxIcon.hasClass('clear')) {
             // Click on X clears the search
             setQuery();

             // TODO fire event so app.js gets notified
           }
         },

         /** As soon as anything is typed, force spyglass icon **/
         onKeydown = function() {
           currentIconClass = 'icon search';
           searchBoxIcon.attr('class', currentIconClass);
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
         },

         /** En- or disables the loading indicator **/
         setLoading = function(loading) {
           if (loading)
             searchBoxIcon.attr('class', 'icon loading');
           else
             searchBoxIcon.attr('class', currentIconClass);
         };

    searchBoxForm.submit(onSubmit);
    searchBoxForm.keydown(onKeydown);

    searchBoxIcon.click(onIconClicked);

    autocomplete.on('selectOption', onSelectOption);

    this.setQuery = setQuery;
    this.setLoading = setLoading;

    HasEvents.apply(this);
  };
  SearchBox.prototype = Object.create(HasEvents.prototype);

  return SearchBox;

});
