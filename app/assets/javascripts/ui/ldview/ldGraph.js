define([], function() {

  var DUMMY_GRAPH = {
    nodes: [
      { "id": "1" },
      { "id": "2" },
      { "id": "3" },
      { "id": "4" },
      { "id": "5" }
    ],

    links: [
      { "source": "1", "target": "2" },
      { "source": "1", "target": "3" },
      { "source": "1", "target": "5" },
      { "source": "2", "target": "4" },
      { "source": "4", "target": "5" }
    ]
  };

  // Mostly https://bl.ocks.org/mbostock/4062045
  var LDGraph = function(d3, svgEl, item) {

    var onDragStart = function(d) {
          if (!d3.event.active) simulation.alphaTarget(0.3).restart();
          d.fx = d.x;
          d.fy = d.y;
        },

        onDragged = function(d) {
          d.fx = d3.event.x;
          d.fy = d3.event.y;
        },

        onDragEnd = function(d) {
          if (!d3.event.active) simulation.alphaTarget(0);
          d.fx = null;
          d.fy = null;
        },

        graph = DUMMY_GRAPH, // TODO

        svg = d3.select(svgEl),

        simulation = d3.forceSimulation()
          .force("link", d3.forceLink().id(function(d) { return d.id; }))
          .force('charge', d3.forceManyBody())
          .force('center', d3.forceCenter(100, 100)),

        link = svg.append('g')
          .attr("class", "links")
          .selectAll('line')
          .data(graph.links)
          .enter()
            .append('line'),

        node = svg.append('g')
          .attr("class", "nodes")
          .selectAll('circle')
          .data(graph.nodes)
          .enter()
            .append('circle')
            .attr('r', 5)
            .call(d3.drag()
              .on('start', onDragStart)
              .on('drag', onDragged)
              .on('end', onDragEnd)),

        ticked = function() {
          link
            .attr('x1', function(d) { return d.source.x; })
            .attr('y1', function(d) { return d.source.y; })
            .attr('x2', function(d) { return d.target.x; })
            .attr('y2', function(d) { return d.target.y; });

          node
            .attr('cx', function(d) { return d.x; })
            .attr('cy', function(d) { return d.y; });
        },

        stop = function() {
          simulation.stop();
        };

    simulation.nodes(graph.nodes).on('tick', ticked);
    simulation.force('link').links(graph.links);

    this.stop = stop;
  };

  return LDGraph;

});
