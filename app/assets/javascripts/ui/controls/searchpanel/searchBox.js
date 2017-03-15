define(['ui/common/hasEvents'], function(HasEvents) {

  var SearchBox = function(parentEl) {

    var self = this,

        element = jQuery(
           '<div id="searchbox">' +
           '  <form>' +
           '    <input type="text" name="q" autocomplete="off">' +
           '    <span class="icon">&#xf002;</span>' +
           '  </form>' +
           '</div>').appendTo(parentEl),

         /** DOM element shorthands **/
         searchBoxForm = element.find('form'),
         searchBoxInput = searchBoxForm.find('input'),
         searchBoxIcon = searchBoxForm.find('.icon'),

         onSubmit = function() {
           var chars = searchBoxInput.val().trim();

           if (chars.length === 0)
             self.fireEvent('change', false);
           else
             self.fireEvent('change', chars);

           searchBoxInput.blur();
           return false;
         };

    searchBoxForm.submit(onSubmit);
    HasEvents.apply(this);
  };
  SearchBox.prototype = Object.create(HasEvents.prototype);

  return SearchBox;

});
