/**
 * TODO implement deferred handling, request throttling, etc.
 */
define(['ui/common/hasEvents'], function(HasEvents) {

  /** Number of results per page **/
  var PAGE_SIZE = 20;

  var API = function() {

    var self = this,

        searchArgs = {

          query: false,

          filters: {},

          settings: {

            timeHistogram : false,

            termAggregations: false,

            topPlaces: true

          }

        },

        currentOffset = 0,

        // DRY helper
        appendIfExists = function(param, key, url) {
          if (param) return url + '&' + key + '=' + param;
          else return url;
        },

        buildBaseQuery = function() {
          var url = '/api/search?limit=' + PAGE_SIZE;

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

        buildNextPageQuery = function() {
          // TODO more to come later
          return buildBaseQuery() + '&offset=' + (currentOffset + PAGE_SIZE);
        },

        makeRequest = function() {
          // var requestArgs = jQuery.extend({}, searchArgs); // Args at time of query
          // busy = true;
          jQuery.getJSON(buildFirstPageQuery(), function(response) {
            self.fireEvent('update', response);
          });
        },

        loadNextPage = function() {
          jQuery.getJSON(buildNextPageQuery(), function(response) {
            currentOffset = response.offset;

            // TODO different events for new result vs. next page?

            // self.fireEvent('update', response);
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
          makeRequest();
        },

        // Shorthand
        enableAggregations = function() {
          updateSettings({
            timeHistogram    : true,
            termAggregations : true,
          });
        },

        // Shorthand
        disableAggregations = function() {
          updateSettings({
            timeHistogram    : false,
            termAggregations : false,
          });
        };

    this.updateQuery = updateQuery;
    this.updateFilters = updateFilters;
    this.updateSettings = updateSettings;
    this.enableAggregations = enableAggregations;
    this.disableAggregations = disableAggregations;
    this.loadNextPage = loadNextPage;

    HasEvents.apply(this);
  };
  API.prototype = Object.create(HasEvents.prototype);

  return API;

});
