define([
  'ui/common/hasEvents',
  'ui/controls/results/templates'
], function(HasEvents, Templates) {

  var ResultList = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="result-list">' +
            '<div class="rl-header">' +
              '<span class="results-local">4 results at Athenae</span> · ' +
              '<span class="results-all">Show all (19)</span>' +
            '</div>' +
            '<div class="rl-body">' +
              '<ul></ul>' +
              '<div class="rl-wait"><img src="/assets/images/wait-circle.gif"/></div>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        listEl = element.find('ul'),

        waitSpinner = element.find('.rl-wait').hide(),

        // To keep track of the current selection
        currentSelection = false,

        // Flag to record whether page load is currently in progress
        waitingForNextPage = false,

        // Flag to record whether more search result pages are available
        isMoreAvailable = true,

        // See .setSelectedItem below
        lastScrollTop = 0,

        createRow = function(item) {
          var el = Templates.createRow(item).appendTo(listEl);
          el.data('item', item);
        },

        onSelect = function(e) {
          var li = jQuery(e.target).closest('li'),
              item = li.data('item');

          self.fireEvent('select', item);
        },

        onScroll = function() {
          lastScrollTop = element.scrollTop();

          if (isMoreAvailable) {
            var scrollPos = element.scrollTop() + element.innerHeight(),
                scrollBottom = element[0].scrollHeight - waitSpinner.outerHeight();

            if (scrollPos >= scrollBottom && !waitingForNextPage) {
              // Keep a flag, so that no more requests are fired before the next page arrives
              waitingForNextPage = true;
              self.fireEvent('nextPage');
            }
          }
        },

        renderResponse = function(response, append) {
          isMoreAvailable = response.total > (response.offset + response.limit);

          if (isMoreAvailable) waitSpinner.show();
          else waitSpinner.hide();

          if (!append) {
            listEl.empty();
            listEl.scrollTop(0);
          }

          response.items.forEach(createRow);
        },

        setSearchResponse = function(response) {
          renderResponse(response, false);
        },

        setSelectedItem = function(item) {
          // We currently don't do anything with the selected item. However, unfortunately,
          // the element's scroll position jumps when a selection is made due (flexbox...).
          // Therefore we force the scroll position back to what it was before selection
          element.scrollTop(lastScrollTop);
        },

        appendPage = function(response) {
          renderResponse(response, true);
          waitingForNextPage = false;
        };

    element.on('click', 'li', onSelect);
    element.scroll(onScroll);

    this.appendPage = appendPage;
    this.setSearchResponse = setSearchResponse;
    this.setSelectedItem = setSelectedItem;

    HasEvents.apply(this);
  };
  ResultList.prototype = Object.create(HasEvents.prototype);

  return ResultList;

});
