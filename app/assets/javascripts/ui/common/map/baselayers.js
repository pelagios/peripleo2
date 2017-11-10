define([], function(HasEvents) {

  // TODO fetch this information from the server, so we can feed it from the DB
  var LAYERS = [{
    id          : 'AWMC',
    title       : 'Empty Basemap',
    description : 'Geographically accurate basemap of the ancient world by the ' +
      '<a href="http://awmc.unc.edu/wordpress/tiles/" target="_blank">Ancient World Mapping Centre</a>, ' +
      'University of North Caronlina at Chapel Hill.',
    thumb_url   : 'http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/7/68/47.png',
    tile_url    : 'http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png',
    attribution : 'Tiles &copy; <a href="http://mapbox.com/" target="_blank">MapBox</a> | ' +
      'Data &copy; <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors, CC-BY-SA | '+
      'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
      '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'
  },{
    id          : 'DARE',
    title       : 'Ancient Places',
    description : 'Roman Empire base map by the <a href="http://dare.ht.lu.se/" target="_blank">Digital ' +
      'Atlas of the Roman Empire</a>, Lund University, Sweden.',
    thumb_url   : 'http://pelagios.org/tilesets/imperium/7/68/47.png',
    tile_url    : 'http://pelagios.org/tilesets/imperium/{z}/{x}/{y}.png',
    attribution : 'Tiles: <a href="http://imperium.ahlfeldt.se/">DARE 2014</a>',
    min_zoom    :3,
    max_zoom    :11
  },{
    id          : 'OSM',
    title       : 'Modern Places',
    description : 'Modern places and roads via <a href="http://www.openstreetmap.org" target="_blank">OpenStreetMap</a>.',
    thumb_url   : 'http://a.tile.openstreetmap.org/7/68/47.png',
    tile_url    : 'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution : '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
      '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
  },{
    id          : 'AERIAL',
    title       : 'Aerial',
    description : 'Aerial imagery via <a href="https://www.mapbox.com/" target="_blank">Mapbox</a>.',
    thumb_url   : 'http://api.tiles.mapbox.com/v4/mapbox.satellite/7/68/47.png?access_token=pk.eyJ1IjoicGVsYWdpb3MiLCJhIjoiMWRlODMzM2NkZWU3YzkxOGJkMDFiMmFiYjk3NWZkMmUifQ.cyqpSZvhsvBGEBwRfniVrg',
    tile_url    : 'http://api.tiles.mapbox.com/v4/mapbox.satellite/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoicGVsYWdpb3MiLCJhIjoiMWRlODMzM2NkZWU3YzkxOGJkMDFiMmFiYjk3NWZkMmUifQ.cyqpSZvhsvBGEBwRfniVrg',
    attribution : '<a href="https://www.mapbox.com/about/maps/">&copy; Mapbox</a> <a href="http://www.openstreetmap.org/about/">&copy; OpenStreetMap</a>',
    max_zoom    : 22
  }];

  return {

    all : function() {
      return LAYERS;
    },

    getLayer : function(id) {
      return LAYERS.find(function(l) {
        return l.id === id;
      });
    }

  };

});
