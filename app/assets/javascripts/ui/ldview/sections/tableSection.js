define([
  'ui/common/itemUtils'
], function(ItemUtils) {

  var TableSection = function(containerDiv, item) {
    var element = jQuery(
          '<div>' +
            '<div class="ldview-table-header">' +
              '<h3>' + item.title + '</h3>' +
              '<p>' + item.is_conflation_of.length + ' conflated records</p>' +
            '</div>' +
            '<ul class="ldview-table-body"></ul>' +
          '</div>').appendTo(containerDiv),

        recordTemplate =
          '<li>' +
            '<div class="head">' +
              '<span class="toggle icon">&#xf107;</span>' +
              '<h4></h4><p class="identifier"></p>' +
            '</div>' +
            '<div class="body">' +
              '<p class="prop descriptions">' +
                '<h5>Descriptions</h5>' +
                '<ul></ul>' +
              '</p>' +

              '<p class="prop names">' +
                '<h5>Names</h5>' +
                '<ul></ul>' +
              '</p>' +
            '</div>' +
          '</li>',

        recordList = element.find('ul');

        init = function() {
          item.is_conflation_of.forEach(function(record) {
            var li = jQuery(recordTemplate),
                parsed = ItemUtils.parseEntityURI(record.uri);

            li.find('h4').append(record.title);
            li.find('.identifier').append(
              '<a href="' + record.uri + '" target="_blank">' + record.uri + '</a>');

            if (parsed.color)
              li.find('.toggle').css('backgroundColor', parsed.color);

            li.find('.body').hide();
            recordList.append(li);
          });
        };

    init();
  };

  return TableSection;

});
