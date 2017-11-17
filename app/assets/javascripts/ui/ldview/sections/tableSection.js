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
              '<div class="prop descriptions">' +
                '<h5>Descriptions</h5>' +
                '<ul></ul>' +
              '</div>' +

              '<div class="prop names">' +
                '<h5>Names</h5>' +
                '<p></p>' +
              '</div>' +

              '<div class="prop links">' +
                '<h5>Links</h5>' +
                '<ul></ul>' +
              '</div>' +
            '</div>' +
          '</li>',

        recordList = element.find('ul');

        init = function() {
          var initList = function() {
                item.is_conflation_of.forEach(function(record) {
                  var li = jQuery(recordTemplate),
                      descriptionsEl = li.find('.descriptions ul'),
                      namesEl        = li.find('.names p'),
                      linksEl        = li.find('.links ul'),

                      parsed = ItemUtils.parseEntityURI(record.uri);

                  li.find('h4').append(record.title);
                  li.find('.identifier').append(
                    '<a href="' + record.uri + '" target="_blank">' + record.uri + '</a>');

                  if (parsed.color)
                    li.find('.toggle').css('backgroundColor', parsed.color);

                  if (record.descriptions)
                    record.descriptions.forEach(function(d) {
                      descriptionsEl.append('<li>' + d.description + '</li>');
                    });

                  // Just lump all names into a comma-separated list
                  if (record.names)
                    namesEl.append(record.names.map(function(n) {
                      return n.name;
                    }).join(', '));
                  else
                    namesEl.hide();

                  if (record.links)
                    record.links.forEach(function(l) {
                      linksEl.append(
                        '<li class="link">' +
                          l.link_type +
                          ' <a href="' + l.uri + '" target="_blank">' + l.uri + '</a>' +
                        '</li>');
                    });

                  li.find('.body').hide();
                  recordList.append(li);
                });
              },

              onToggle = function(e) {
                var li = jQuery(e.target).closest('li');
                li.find('.body').slideToggle(100);
              };

          initList();

          recordList.on('click', '.head', onToggle);
        };

    init();
  };

  return TableSection;

});
