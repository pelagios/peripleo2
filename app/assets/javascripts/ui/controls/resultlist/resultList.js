define([
  'ui/common/hasEvents'
], function(HasEvents) {

  var ResultList = function(parentEl) {

    var element = jQuery(
          '<div id="result-list">' +
            '<ul></ul>' +
            '<div class="rl-wait"></div>' +
          '</div>').appendTo(parentEl),

        listEl = element.find('ul'),

        createRow = function(item) {
          var el = jQuery(
                '<li>' +
                  '<h3>' + item.title + '</h3>' +
                '</li>').appendTo(listEl);

          // console.log(item);
        },

        update = function(response) {
          listEl.empty();
          response.items.forEach(createRow);
        };

    this.update = update;

    HasEvents.apply(this);
  };
  ResultList.prototype = Object.create(HasEvents.prototype);

  return ResultList;

});
