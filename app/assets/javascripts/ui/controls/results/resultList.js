define([
  'ui/common/hasEvents'
], function(HasEvents) {

  var ResultList = function(parentEl) {

    var self = this,

        element = jQuery(
          '<div id="result-list">' +
            '<ul></ul>' +
            '<div class="rl-wait"><img src="/assets/images/wait-circle.gif"/></div>' +
          '</div>').appendTo(parentEl),

        listEl = element.find('ul'),

        waitSpinner = element.find('.rl-wait').hide(),

        waitingForNextPage = false,

        createRow = function(item) {
          var el = jQuery(
                '<li>' +
                  '<h3>' + item.title + '</h3>' +
                '</li>').appendTo(listEl);

          // TODO should we really bind to the DOM element?
          // TODO or just use data-id and then GET via the API?
          el.data('item', item);
        },

        onSelect = function(e) {
          var li = jQuery(e.target).closest('li'),
              item = li.data('item');

          self.fireEvent('select', item);
        },

        onScroll = function() {
          var scrollPos = element.scrollTop() + element.innerHeight(),
              scrollBottom = element[0].scrollHeight - waitSpinner.outerHeight();

          if (scrollPos >= scrollBottom && !waitingForNextPage) {
            // Keep a flag, so that no more requests are fired before the next page arrives
            waitingForNextPage = true;
            self.fireEvent('nextPage');
          }
        },

        setResponse = function(response) {
          var isMoreAvailable = response.total > (response.offset + response.limit);

          if (isMoreAvailable) waitSpinner.show();
          else waitSpinner.hide();

          listEl.empty();
          response.items.forEach(createRow);
        };

    element.on('click', 'li', onSelect);
    element.scroll(onScroll);

    this.setResponse = setResponse;

    HasEvents.apply(this);
  };
  ResultList.prototype = Object.create(HasEvents.prototype);

  return ResultList;

});
