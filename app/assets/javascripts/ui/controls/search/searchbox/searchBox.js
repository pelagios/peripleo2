define([
  'ui/common/hasEvents',
  'ui/controls/search/searchbox/autocomplete',
  'ui/controls/search/searchbox/indicators/indicatorRow'
], function(HasEvents, Autocomplete, IndicatorRow) {

  var SPINNER_STOP_DELAY = 300;

  var SearchBox = function(parentEl) {

    var self = this,

        element = jQuery(
           '<div id="searchbox">' +
             '<form>' +
               '<input type="text" name="q" autocomplete="off">' +
               '<span class="icon state search"></span>' +
             '</form>' +
           '</div>').appendTo(parentEl),

         searchBoxForm = element.find('form'),
         searchBoxInput = searchBoxForm.find('input'),
         searchBoxIcon = searchBoxForm.find('.icon.state'),

         indicatorRow = new IndicatorRow(searchBoxForm, searchBoxInput),

         autocomplete = new Autocomplete(searchBoxForm, searchBoxInput),

         /**
          * We introduce a little delay for stopping the load spinner, in order
          * to avoid jittery on/off behavior when many search requests happen
          * in fast succession, e.g. when dragging the time slider. This var
          * keeps track of the timeout function
          */
         stopSpinner = false,

         /** En- or disables the loading spinner **/
         setLoading = function(loading) {
           if (loading) {
             if (stopSpinner) {
               clearTimeout(stopSpinner);
               stopSpinner = false;
             }
             searchBoxIcon.attr('class', 'icon state loading');
           } else if (!loading && !stopSpinner) {
             stopSpinner = setTimeout(refreshIconState, SPINNER_STOP_DELAY);
           }
         },

         refreshIconState = function() {
           var noQuery = searchBoxInput.val().trim() === '';
           if (indicatorRow.isEmpty() && noQuery)
             searchBoxIcon.attr('class', 'icon state search');
           else
             searchBoxIcon.attr('class', 'icon state clear');
         },

         /** Only click on the spyglass icon triggers a search, not on the load spinner **/
         onIconClicked = function() {
           if (searchBoxIcon.hasClass('search')) {
             onSubmit();
           } else if (searchBoxIcon.hasClass('clear')) {
             indicatorRow.clear();
             autocomplete.clear(); // Also clears the query
             refreshIconState();
             self.fireEvent('clearAll');
           }
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

         /** Sets the search state WITHOUT FIRING A CHANGE EVENT **/
         setSearch = function(search) {
           if (search.query)
             searchBoxInput.val(search.query);
           else
             searchBoxInput.val('');
         },

         updateIndicators = function(filterSetting) {
           indicatorRow.update(filterSetting);
         },

         removeIndicators = function(filterType, opt_identifier) {
           indicatorRow.remove(filterType, opt_identifier);
         };

    searchBoxForm.submit(onSubmit);
    searchBoxIcon.click(onIconClicked);

    autocomplete.on('selectOption', onSelectOption);

    this.setLoading = setLoading;
    this.setSearch = setSearch;
    this.updateIndicators = updateIndicators;
    this.removeIndicators = removeIndicators;
    this.showTimefilterIndicator = indicatorRow.showTimefilterIndicator;
    this.hideTimefilterIndicator = indicatorRow.hideTimefilterIndicator;

    HasEvents.apply(this);
  };
  SearchBox.prototype = Object.create(HasEvents.prototype);

  return SearchBox;

});
