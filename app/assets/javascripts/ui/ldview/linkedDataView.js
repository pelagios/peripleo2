define([
  'ui/ldview/sections/graphSection',
  'ui/ldview/sections/mapSection',
  'ui/ldview/sections/tableSection'
], function(Graph, Map, Table) {

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
                  '<div class="graph-section"><svg width="100%" height="100%"></svg></div>' +
                  '<div class="map-section"></div>' +
                  '<div class="table-section"></div>' +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>').appendTo(document.body),

        /** Loads D3 asynchronously (if needed) and initializes the UI **/
        init = function() {
          var graph, map;

          element.find('.close').click(function() {
            if (graph) graph.stop();
            element.remove();
          });

          require(['d3'], function(d3) {
            graph = new Graph(d3, element.find('.ldview-body svg'), item);
            map = new Map(element.find('.ldview-body .map-section')[0], item);
            table = new Table(element.find('.ldview-body .table-section'), item);
          });
        };

    init();
  };

  return LinkedDataView;

});
