define([
  'ui/common/formatting',
  'ui/common/hasEvents',
  'ui/controls/results/templates'
], function(Formatting, HasEvents, Templates) {

  var ResultList = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="result-list">' +
            '<div class="rl-body">' +
              '<ul></ul>' +
              '<div class="rl-wait"><img src="/assets/images/wait-circle.gif"/></div>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        bodyEl = element.find('.rl-body'),
        listEl = bodyEl.find('ul'),
        waitSpinner = bodyEl.find('.rl-wait').hide(),

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
          lastScrollTop = bodyEl.scrollTop();

          if (isMoreAvailable) {
            var scrollPos = bodyEl.scrollTop() + bodyEl.innerHeight(),
                scrollBottom = bodyEl[0].scrollHeight - waitSpinner.outerHeight();

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
            bodyEl.scrollTop(0);

            // If the response replaces the current list, remove selection
            currentSelection = false;
          }

          response.items.forEach(createRow);
        },

        setSearchResponse = function(response) {
          renderResponse(response, false);
        },

        setSelectedItem = function(item) {
          var select = function() {
                var setCurrentSelection = function() {
                      // TODO there are various ways to optimize if needed (indexed lookup etc.)
                      listEl.children('li').each(function(idx, node) {
                        var el = jQuery(node),
                            docId = el.data('item').doc_id;

                        if (docId === item.doc_id) {
                          el.addClass('selected');
                          currentSelection = el;
                          return false;
                        }
                      });
                    };

                if (currentSelection) {
                  if (currentSelection.data('item').doc_id !== item.doc_id) {
                    currentSelection.removeClass('selected');
                    setCurrentSelection();
                  }
                } else {
                  setCurrentSelection();
                }
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

          // The element's scroll position jumps when a selection is made (flexbox...).
          // Therefore we force the scroll position back to what it was before selection
          var scrollTop = lastScrollTop;
          setTimeout(function() { bodyEl[0].scrollTop = scrollTop; }, 1);
        },

        appendPage = function(response) {
          renderResponse(response, true);
          waitingForNextPage = false;
        };

    bodyEl.on('click', 'li', onSelect);
    bodyEl.scroll(onScroll);

    this.appendPage = appendPage;
    this.setSearchResponse = setSearchResponse;
    this.setSelectedItem = setSelectedItem;

    HasEvents.apply(this);
  };
  ResultList.prototype = Object.create(HasEvents.prototype);

  return ResultList;

});
