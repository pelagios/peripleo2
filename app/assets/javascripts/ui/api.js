/**
 * TODO implement deferred handling, request throttling, etc.
 */
define(['ui/common/hasEvents'], function(HasEvents) {

  /** Number of results per page **/
  var LIMIT = 20;

  var API = function() {

    var searchArgs = {

          query: false,

          filters: {},

          settings: {

            timeHistogram : false,

            termAggregations: false,

            topPlaces: true

          }

        },

        // DRY helper
        appendIfExists = function(param, key, url) {
          if (param) return url + '&' + key + '=' + param;
          else return url;
        },

        buildBaseQuery = function() {
          var url = '/api/search?limit=' + LIMIT;

          // TODO more to come later
          url = appendIfExists(searchArgs.query, 'q', url);

          return url;
        },

        buildFirstPageQuery = function() {
          var url = buildBaseQuery(),
              settings = searchArgs.settings;

          // First page query includes aggregations
          url = appendIfExists(settings.timeHistogram, 'time_histogram', url);
          url = appendIfExists(settings.termAggregations, 'facets', url);
          url = appendIfExists(settings.topPlaces, 'top_places', url);

          return url;
        },

        buildNextPageQuery = function(offset) {
          // TODO more to come later
          return buildBaseQuery();
        },

        makeRequest = function() {
          var requestArgs = jQuery.extend({}, searchArgs); // Args at time of query
          // busy = true;
          jQuery.getJSON(buildFirstPageQuery(), function(response) {
            console.log(response);
          });
        },

        updateQuery = function(query) {
          searchArgs.query = query;
          makeRequest();
        },

        updateFilters = function(diff) {
          jQuery.extend(searchArgs.filters, diff);
        },

        updateSettings = function(diff) {
          jQuery.extend(searchArgs.settings, diff);
        };

    this.updateQuery = updateQuery;
    this.updateFilters = updateFilters;
    this.updateSettings = updateSettings;

    HasEvents.apply(this);
  };
  API.prototype = Object.create(HasEvents.prototype);

  return API;

});
