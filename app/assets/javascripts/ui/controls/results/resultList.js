define([
  'ui/common/hasEvents',
  'ui/controls/results/templates'
], function(HasEvents, Templates) {

  var SLIDE_DURATION = 100;

  var ResultList = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="result-list">' +
            '<div class="rl-header">' +
              '<span class="results-local"></span>' +
              '<span class="results-all"><a href="#">All results</a></span>' +
            '</div>' +
            '<div class="rl-body">' +
              '<ul></ul>' +
              '<div class="rl-wait"><img src="/assets/images/wait-circle.gif"/></div>' +
            '</div>' +
          '</div>').appendTo(parentEl),

        headerEl = element.find('.rl-header').hide(),
        resultsLocalEl = headerEl.find('.results-local'),
        resultsAllEl = headerEl.find('.results-all'),

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
          }

          response.items.forEach(createRow);
        },

        setSearchResponse = function(response) {
          if (headerEl.is(':visible'))
            headerEl.velocity('slideUp', { duration: SLIDE_DURATION });

          renderResponse(response, false);
        },

        setFilteredResponse = function(response, reference) {
          resultsLocalEl.html(
            response.total + ' results for <a href="#">' + reference.title + '</a>');
          headerEl.velocity('slideDown', { duration: SLIDE_DURATION });
          renderResponse(response, false);
        },

        setSelectedItem = function(item) {
          // We currently don't do anything with the selected item. However, unfortunately,
          // the element's scroll position jumps when a selection is made due (flexbox...).
          // Therefore we force the scroll position back to what it was before selection
          bodyEl.scrollTop(lastScrollTop);
        },

        appendPage = function(response) {
          renderResponse(response, true);
          waitingForNextPage = false;
        },

        onExitFilteredSearch = function() {
          self.fireEvent('exitFilteredSearch');
        };

    bodyEl.on('click', 'li', onSelect);
    bodyEl.scroll(onScroll);

    resultsAllEl.click(onExitFilteredSearch);

    this.appendPage = appendPage;
    this.setSearchResponse = setSearchResponse;
    this.setFilteredResponse = setFilteredResponse;
    this.setSelectedItem = setSelectedItem;

    HasEvents.apply(this);
  };
  ResultList.prototype = Object.create(HasEvents.prototype);

  return ResultList;

});
