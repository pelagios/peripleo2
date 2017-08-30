define([], function() {

  var LinkedDataView = function(item) {

    var element = jQuery(
          '<div class="clicktrap">' +
            '<div class="modal-wrapper ldview-wrapper">' +
              '<div class="modal ldview">' +
                '<div class="modal-header ldview-header">' +
                  '<h2>Linked Data View</h2>' +
                  '<button class="icon tonicons close">&#xe897;</button>' +
                '</div>' +
                '<div class="modal-body ldview-body">' +

                  // TODO

                '</div>' +
              '</div>' +
            '</div>' +
          '</div>')/*.hide()*/.appendTo(document.body),

        init = function() {
          element.find('.close').click(function() { element.remove(); });
        };

    init();
  };

  return LinkedDataView;

});
