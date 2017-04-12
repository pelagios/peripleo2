/**
 * TODO implement deferred handling, request throttling, etc.
 */
define(['ui/common/hasEvents'], function(HasEvents) {

      /** Number of results per page **/
  var PAGE_SIZE = 20,

      // To throttle traffic, we'll stay idle between requests for this time in millis
      IDLE_MS = 200;

  var Search = function() {

    var self = this,

        searchArgs = {

          query: false,

          filters: {},

          timerange: { from: false, to : false },

          settings: {

            timeHistogram : false,

            termAggregations: false,

            topPlaces: true

          }

        },

        currentOffset = 0,

        // Are we currently waiting for an API response?
        busy = false,

        // Did additional requests arrive while busy?
        requestPending = false,

        // DRY helper
        appendIfExists = function(param, key, url) {
          if (param) return url + '&' + key + '=' + param;
          else return url;
        },

        buildBaseQuery = function() {
          var url = '/api/search?limit=' + PAGE_SIZE;

          url = appendIfExists(searchArgs.query, 'q', url);
          url = appendIfExists(searchArgs.timerange.from, 'from', url);
          url = appendIfExists(searchArgs.timerange.to, 'to', url);

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

          var handlePending = function() {
                if (requestPending) {
                  request();
                  requestPending = false;
                } else {
                  // Throttling: no request pending right now? Wait a bit
                  setTimeout(function() {
                    if (requestPending)
                      handlePending();
                    else
                      // Still nothing? Clear busy flag.
                      busy = false;
                  }, IDLE_MS);
                }
              },

              request = function() {
                jQuery.getJSON(buildFirstPageQuery(), function(response) {
                  self.fireEvent('update', response);
                }).always(handlePending);
              };

          if (busy) {
            requestPending = true;
          } else {
            busy = true;
            request();
          }
        },

        loadNextPage = function() {
          jQuery.getJSON(buildNextPageQuery(), function(response) {
            currentOffset = response.offset;

            // TODO different events for new result vs. next page?

            // self.fireEvent('update', response);
            // console.log(response);
          });
        },

        updateQuery = function(query) {
          searchArgs.query = query;
          makeRequest();
        },

        updateTimerange = function(range) {
          searchArgs.timerange = range;
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
    this.updateTimerange = updateTimerange;
    this.updateFilters = updateFilters;
    this.updateSettings = updateSettings;
    this.enableAggregations = enableAggregations;
    this.disableAggregations = disableAggregations;
    this.loadNextPage = loadNextPage;

    HasEvents.apply(this);
  };
  Search.prototype = Object.create(HasEvents.prototype);

  return Search;

});
