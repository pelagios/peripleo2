require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'ui/controls/searchpanel',
  'ui/map/map',
  'ui/api'
], function(SearchPanel, Map, API) {

  jQuery(document).ready(function() {
    var body = jQuery(document.body),

        mapDiv = jQuery('<div id="map"></div>').appendTo(body),

        controlsDiv = jQuery('<div id="controls"></div>').appendTo(body),

        map = new Map(mapDiv[0]),

        searchPanel = new SearchPanel(controlsDiv),

        api = new API();

    searchPanel.on('queryChange', api.updateQuery);
    searchPanel.on('filterChange', api.updateFilters);
    searchPanel.on('settingsChange', api.updateSettings);
  });

});
