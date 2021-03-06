define(['ui/common/itemUtils'], function(ItemUtils) {

  var NODE_RADIUS = 7,

      KNOWN_NAMESPACES = [
        { pattern: 'http://dbpedia.org/resource/',       shortcode: 'dbpedia' },
        { pattern: 'http://www.livius.org/place/',       shortcode: 'livius' },
        { pattern: 'http://www.trismegistos.org/place/', shortcode: 'trismegistos' },
        { pattern: 'http://www.wikidata.org/entity/',    shortcode: 'wikidata' },
        { pattern: 'http://viaf.org/viaf/',              shortcode: 'viaf' }
      ],

      formatURI = function(url) {
        var namespace = KNOWN_NAMESPACES.find(function(ns) {
              return url.indexOf(ns.pattern) === 0;
            });

        if (namespace)
          return namespace.shortcode + ':' + url.substring(namespace.pattern.length);
        else
          return url;
      },

      computeGraph = function(item) {

            /** All links, derived from SKOS close-/exactMatches **/
        var links = item.is_conflation_of.reduce(function(links, record) {

              var getURIs = function(type) {
                    var links = record.links || [];
                        toReturn = [];

                    links.forEach(function(l) {
                      if (l.link_type === type)
                        toReturn.push(l.uri);
                    });

                    return toReturn;
                  },

                  closeMatches = getURIs('closeMatch'),
                  exactMatches = getURIs('exactMatch'),

                  toLinks = function(uris, link) {
                    return uris.map(function(uri) {
                      return { source: record.uri, target: uri, link: link };
                    });
                  },

                  closeLinks = toLinks(closeMatches, 'closeMatch'),
                  exactLinks = toLinks(exactMatches, 'exactMatch');

              return links.concat(closeLinks.concat(exactLinks));
            }, []),

            /** Nodes derived from the records in the item **/
            knownNodes = item.is_conflation_of.map(function(record) {
              return ItemUtils.parseEntityURI(record.uri);
            }),

            /** All nodes including 'anonymous' ones based on links to URIs outside of Peripleo **/
            nodes = links.reduce(function(nodes, link) {
              var target = link.target,

                  nodeExists = nodes.find(function(node) {
                    return node.uri === target;
                  });

              if (!nodeExists)
                nodes.push({ uri: link.target, is_anonymous: true });

              return nodes;
            }, knownNodes);

        return { nodes: nodes, links: links };
      };

  var Legend = function(d3, svgEl) {

    // https://stackoverflow.com/questions/35516083/how-to-make-a-dashed-line-legend-with-d3-js
    var svg = d3.select(svgEl[0]).append('g'),

        legend = svg.selectAll('.legend')
          .data(['exactMatch', 'closeMatch'])
        .enter().append('g')
          .attr('class', 'legend')
          .attr('transform', function(d, i) { return 'translate(0,' + (15 + i * 25) + ')'; });

      legend.append('line')//making a line for legend
          .attr('x1', 20)
          .attr('x2', 60)
          .attr('y1', 10)
          .attr('y2', 10)
          .attr('marker-end', 'url(#arrow)')
          .attr('class', function(c) { return c; });

      svg.append('svg:defs').selectAll('marker')
        .data(['arrow'])
        .enter().append('marker')
          .attr('id', String)
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 0)
          .attr('refY', 0)
          .attr('markerWidth', 5)
          .attr('markerHeight', 6)
          .attr('orient', 'auto')
          .append('path')
            .attr('d', 'M0,-5L10,0L0,5');

      legend.append('text')
          .attr('x', 80)
          .attr('y', 0)
          .attr('dy', '14px')
          .text(function(d) { return 'skos:' + d; });
  };

  // Mostly http://bl.ocks.org/mbostock/4062045
  var GraphSection = function(d3, svgEl, item) {

    var graph = computeGraph(item),

        onDragStart = function(d) {
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

        svg = d3.select(svgEl[0]).append('g'),

        simulation = d3.forceSimulation()
          .force('link', d3.forceLink().id(function(d) { return d.uri; }))
          .force('charge', d3.forceManyBody().strength(-700).distanceMin(0).distanceMax(300))
          .force('center', d3.forceCenter(svgEl.width() / 2, svgEl.height() / 2)),

        links = svg.append('g')
          .selectAll('line')
          .data(graph.links)
          .enter()
            .append('line')
            .attr('class', function(d) { return d.link; })
            .attr('marker-end', 'url(#end)'),

        nodes = svg.selectAll('.node')
          .data(graph.nodes)
          .enter()
            .append('g')
            .attr('class', function(d) {
              return (d.shortcode) ? 'node ' + d.shortcode : 'node';
            })
            .call(d3.drag()
              .on('start', onDragStart)
              .on('drag', onDragged)
              .on('end', onDragEnd)),

        ticked = function() {
          links
            .attr('x1', function(d) { return d.source.x; })
            .attr('y1', function(d) { return d.source.y; })
            .attr('x2', function(d) { return d.target.x; })
            .attr('y2', function(d) { return d.target.y; });

          nodes
            .attr('transform', function(d) { return 'translate(' + d.x + ',' + d.y + ')'; });
        },

        stop = function() {
          simulation.stop();
        };

    // Somehow, I hate D3 syntax...
    svg.append('svg:defs').selectAll('marker')
        .data(['end'])
        .enter().append('marker')
          .attr('id', String)
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 16)
          .attr('refY', 0)
          .attr('markerWidth', 5)
          .attr('markerHeight', 6)
          .attr('orient', 'auto')
          .append('path')
            .attr('d', 'M0,-5L10,0L0,5');

    nodes.append('circle')
      .attr('r', NODE_RADIUS)
      .attr('class', function(d) { return (d.is_anonymous) ? 'anonymous' : ''; })
      .attr('style', function(d) { return (d.color) ? 'fill:' + d.color : ''; });

    nodes.append('title')
      .text(function(d) { return d.uri; });

    nodes.append('svg:a')
      .attr('xlink:href', function(d) { return (d.uri); })
      .attr('target', '_blank')
      .append('text')
        .attr('dx', 15)
        .attr('dy', '.35em')
        .text(function(d) {
          if (d.isKnownAuthority) {
            return d.shortcode + ':' + d.id;
          } else {
            return formatURI(d.uri);
          }
      });

    simulation.nodes(graph.nodes).on('tick', ticked);
    simulation.force('link').links(graph.links);

    new Legend(d3, svgEl);

    this.stop = stop;
  };

  return GraphSection;

});
