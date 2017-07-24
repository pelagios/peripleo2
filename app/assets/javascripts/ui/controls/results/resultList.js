define([
  'ui/common/formatting',
  'ui/common/hasEvents',
  'ui/controls/results/templates'
], function(Formatting, HasEvents, Templates) {

  var ResultList = function(parentEl) {

    var self = this,

        container = jQuery(
          '<div id="result-list">' +
            '<div class="rl-inner">' +
              '<ul></ul>' +
              '<div class="rl-wait"><img src="/assets/images/wait-circle.gif"/></div>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        element = container.find('.rl-inner'),
        list    = container.find('ul'),

        waitSpinner = container.find('.rl-wait').hide(),

        /** Keeps track of the current selection **/
        currentSelection = false,

        /** Flag to record whether page load is currently in progress **/
        waitingForNextPage = false,

        /** Flag to record whether more search result pages are available **/
        isMoreAvailable = true,

        /** Workaround - see .setSelectedItem below **/
        lastScrollTop = 0,

        /** Renders a new search response, either by replacing or appending to the current list **/
        renderResponse = function(response, append) {
          isMoreAvailable = response.total > (response.offset + response.limit);

          // Show or hide wait spinner
          if (isMoreAvailable) waitSpinner.show();
          else waitSpinner.hide();

          if (!append) {
            list.empty();
            element.scrollTop(0);
            currentSelection = false;
          }

          response.items.forEach(function(item) {
            var el = Templates.createRow(item).appendTo(list);
            el.data('item', item);
          });
        },

        setSearchResponse = function(response) {
          renderResponse(response, false);
        },

        appendNextPage = function(response) {
          renderResponse(response, true);
          waitingForNextPage = false;
        },

        /** Selects the specified item and scrolls it into view **/
        setSelectedItem = function(item) {

              // Helper to set find the item in the list
          var findListElement = function() {
                var found = false;

                // This could be optimized if needed with an indexed lookup
                list.children('li').each(function(idx, node) {
                  var el = jQuery(node),
                      docId = el.data('item').doc_id;

                      if (docId === item.doc_id) {
                        found = el;
                        return false;
                      }
                });

                return found;
              },

              select = function() {
                var newSelection = findListElement();

                // Remove previous selection, if needed
                if (currentSelection && currentSelection.data('item').doc_id !== item.doc_id)
                    currentSelection.removeClass('selected');

                if (newSelection)
                  newSelection.addClass('selected');

                currentSelection = newSelection;
              },

              deselect = function() {
                if (currentSelection) {
                  currentSelection.removeClass('selected');
                  currentSelection = false;
                }
              };

          if (item)
            select();
          else
            deselect();

          // The element's scroll position jumps when a selection is made (thanks to flexbox...).
          // Therefore we force the scroll position back to what it was before selection
          var scrollTop = lastScrollTop;
          setTimeout(function() { element[0].scrollTop = scrollTop; }, 1);
        },

        /** Handles list selection and forwards the event up the component hierarchy **/
        onSelect = function(e) {
          var li = jQuery(e.target).closest('li'),
              item = li.data('item');

          self.fireEvent('select', item);
        },

        /** Triggers next page load in case there's more **/
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

        close = function() {
          list.empty();
        };

    element.on('click', 'li', onSelect);
    element.scroll(onScroll);

    this.setSearchResponse = setSearchResponse;
    this.setSelectedItem = setSelectedItem;
    this.appendNextPage = appendNextPage;
    this.close = close;

    HasEvents.apply(this);
  };
  ResultList.prototype = Object.create(HasEvents.prototype);

  return ResultList;

});
