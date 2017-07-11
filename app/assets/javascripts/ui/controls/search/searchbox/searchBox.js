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

         /** En- or disables the loading spinner **/
         setLoading = function(loading) {
           if (loading)
             searchBoxIcon.attr('class', 'icon loading');
           else
             searchBoxIcon.attr('class', 'icon search');
         },

         /** Only click on the spyglass icon triggers a search, not on the load spinner **/
         onIconClicked = function() {
           if (searchBoxIcon.hasClass('search'))
             onSubmit();
         },

         /** Minimal cleanup when search is triggered **/
         onSubmit = function() {
           var chars = searchBoxInput.val().trim();

           if (chars.length === 0)
             self.fireEvent('change', false);
           else
             self.fireEvent('change', chars);

           searchBoxInput.blur();
           return false;
         },

         /**
          * The user picked an auto-suggest option. Handle differently, depending on
          * whether it was a suggested item, or just a suggested search phrase.
          */
         onSelectOption = function(option) {
           if (option.identifier)
             self.fireEvent('selectSuggestOption', option.identifier);
           else
             onSubmit();
         },

         /** Sets the query string WITHOUT FIRING A CHANGE EVENT **/
         setQuery = function(query) {
           if (query) searchBoxInput.val(query);
           else searchBoxInput.val('');
         };

    searchBoxForm.submit(onSubmit);
    searchBoxIcon.click(onIconClicked);

    autocomplete.on('selectOption', onSelectOption);

    this.setLoading = setLoading;
    this.setQuery = setQuery;

    HasEvents.apply(this);
  };
  SearchBox.prototype = Object.create(HasEvents.prototype);

  return SearchBox;

});
