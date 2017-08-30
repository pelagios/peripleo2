define([
  'ui/ldview/ldGraph'
], function(LDGraph) {

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
                  '<div class="graph-container">' +
                    '<svg width="500" height="400"></svg>' +
                  '</div>' +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>').appendTo(document.body),

        /** Loads D3 asynchronously (if needed) and initializes the UI **/
        init = function() {
          var graph;

          element.find('.close').click(function() {
            if (graph) graph.stop();
            element.remove();
          });

          require(['d3'], function(d3) {
            graph = new LDGraph(d3, element.find('.ldview-body svg')[0], item);
          });
        };

    init();
  };

  return LinkedDataView;

});
