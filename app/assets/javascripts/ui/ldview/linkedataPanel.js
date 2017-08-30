define([], function() {

  var LinkedDataPanel = function() {

    var element = jQuery(
          '<div class="clicktrap">' +
            '<div class="modal-wrapper ldview-wrapper">' +
              '<div class="modal ldview">' +
                '<div class="modal-header ldview-header">' +
                  '<h2>Linked Data View</h2>' +
                  '<button class="icon tonicons cancel">&#xe897;</button>' +
                '</div>' +
                '<div class="modal-body ldview-body">' +

                  // TODO

                '</div>' +
              '</div>' +
            '</div>' +
          '</div>').hide().appendTo(document.body);

  };

  return LinkedDataPanel;

});
