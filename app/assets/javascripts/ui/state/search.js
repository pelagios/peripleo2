define([], function() {

      /** Number of results per page **/
  var PAGE_SIZE = 20,

      /** To throttle traffic, we'll stay idle between requests for this time in millis **/
      IDLE_MS = 200;

  var Search = function() {

    var self = this,

        searchArgs = {
          query     : false,
          filters   : { }, // key -> [ value, value, value, ...]
          timerange : { from: false, to : false },
          bbox      : false,
          settings  : {
            timeHistogram    : false,
            termAggregations : false,
            topReferenced    : true
          }
        },

        currentOffset = 0,

        /** Are we currently waiting for an API response? **/
        busy = false,

        /** Last pending request that arrived while busy **/
        pendingRequest = false,

        /** DRY helper **/
        appendIfExists = function(param, key, url) {
          if (param) return url + '&' + key + '=' + param;
          else return url;
        },

        /** Builds the common parts of all query URLs **/
        buildBaseQuery = function(opt_limit) {
          var limit = (opt_limit) ? opt_limit : PAGE_SIZE,
              url = '/api/search?limit=' + limit;

          url = appendIfExists(searchArgs.query, 'q', url);

          for (var f in searchArgs.filters) {
            if (searchArgs.filters[f])
              url += '&' + f + '=' + encodeURIComponent(searchArgs.filters[f]);
          }

          url = appendIfExists(searchArgs.timerange.from, 'from', url);
          url = appendIfExists(searchArgs.timerange.to, 'to', url);
          url = appendIfExists(searchArgs.bbox, 'bbox', url);

          return url;
        },

        /**
         * Builds the initial request.
         *
         * The inital request includes top_referenced entities, aggregations and
         * time histogram (if needed) and starts at offset = 0.
         */
        buildFirstPageQuery = function(settings) {
          var url = buildBaseQuery();

          // First page query includes aggregations
          url = appendIfExists(settings.timeHistogram, 'time_histogram', url);
          url = appendIfExists(settings.termAggregations, 'facets', url);
          url = appendIfExists(settings.topReferenced, 'top_referenced', url);

          // Reset offset for subsequent next page queries
          currentOffset = 0;

          return url;
        },

        /** Next page requests are just the base query with offset **/
        buildNextPageQuery = function(opt_limit) {
          return buildBaseQuery(opt_limit) + '&offset=' + (currentOffset + PAGE_SIZE);
        },

        /**
         * Request handling (and throttling) for loading a new first result page.
         *
         * Triggers a request through the API, or stashes the request for later if
         * we're currently busy.
         */
        loadFirstPage = function(opt_settings) {

          var deferred = jQuery.Deferred(),

              handlePending = function() {
                if (pendingRequest) {
                  request(pendingRequest.deferred, pendingRequest.opt_settings);
                  pendingRequest = false;
                } else {
                  // Throttling: no request pending right now? Wait a bit
                  setTimeout(function() {
                    if (pendingRequest)
                      handlePending();
                    else
                      // Still nothing? Clear busy flag.
                      busy = false;
                  }, IDLE_MS);
                }
              },

              request = function(deferred, opt_settings) {
                var settings = (opt_settings) ?
                      jQuery.extend({}, searchArgs.settings, opt_settings) :
                      searchArgs.settings,

                    requestArgs = jQuery.extend({}, searchArgs);

                // Clone request args at time of request, so we can add them to the response
                requestArgs.settings = jQuery.extend(true, {}, requestArgs.settings, settings);

                jQuery.getJSON(buildFirstPageQuery(settings), function(response) {

                  // When a response doesn't have top-referenced places (despite the search
                  // args requesting them) we automatically follow up with a next-page
                  // request of length 600, so we can plot more item geometries
                  var makeFollowUpReq =
                    requestArgs.settings.topReferenced &&
                    (!response.top_referenced.PLACE || response.top_referenced.PLACE.length < 600);

                  response.request_args = requestArgs;

                  if (makeFollowUpReq)
                    loadNextPage(600).done(function(nextPage) {
                      // Merge next page with first page result
                      response.items = response.items.concat(nextPage.items);
                      deferred.resolve(response);
                    });
                  else
                    deferred.resolve(response);
                }).always(handlePending);
              };

          if (busy) {
            pendingRequest = { deferred: deferred, opt_settings: opt_settings };
          } else {
            busy = true;
            request(deferred, opt_settings);
          }

          return deferred.promise(this);
        },

        /** Request handling for loading subsequent result pages **/
        loadNextPage = function(opt_limit) {
          var deferred = jQuery.Deferred();

          jQuery.getJSON(buildNextPageQuery(opt_limit), function(response) {
            currentOffset = response.offset;
            deferred.resolve(response);
          });

          return deferred.promise(this);
        },

        /** Sets new search args (completely replacing the previous) and triggers a new search **/
        setArgs = function(args, makeReq) {
          searchArgs = args;
          if (makeReq) return loadFirstPage();
        },

        /** Returns a clone of the current search args **/
        getArgs = function() {
          return jQuery.extend({}, searchArgs);
        },

        /** Clears all search args, optionally triggering a new request **/
        clear = function(makeReq) {
          searchArgs.query = false;
          searchArgs.filters = {};
          searchArgs.timerange = { from: false, to : false };
          if (makeReq) return loadFirstPage();
        },

        /** Clears the filters, optionally triggering a new request **/
        clearFilters = function(makeReq) {
          searchArgs.filters = {};
          if (makeReq) return loadFirstPage();
        },

        /** Updates the filters, optionally triggering a new request **/
        updateFilters = function(diff, makeReq) {
          jQuery.extend(searchArgs.filters, diff);
          if (makeReq) return loadFirstPage();
        },

        /**
         * Sets the time range and triggers a new request.
         *
         * Note: there's no need to recompute the histogram in case the time range changes,
         * so we force the settings to 'time_histogram=false'.
         */
        setTimerange = function(range) {
          searchArgs.timerange = range;
          return loadFirstPage({ timeHistogram: false });
        },

        /** Sets a new query phrase, optionally triggering a new request **/
        setQuery = function(query, makeReq) {
          searchArgs.query = query;
          if (makeReq) return loadFirstPage();
        },

        /** Returns the current search query **/
        getQuery = function() {
          return searchArgs.query;
        },

        /** Sets termAggregations and time histogram setting, optionally triggering a new request **/
        setAggregationsEnabled = function(enabled, makeReq) {
          jQuery.extend(searchArgs.settings,{
            timeHistogram    : enabled,
            termAggregations : enabled,
          });
          if (makeReq) return loadFirstPage();
        },

        /** Updates the viewport bbox, optionally triggering a new request **/
        setViewport = function(bounds, makeReq) {
          searchArgs.bbox = bounds;
          if (makeReq) return loadFirstPage();
        };

    this.setArgs = setArgs;
    this.getArgs = getArgs;
    this.clear = clear;
    this.clearFilters = clearFilters;
    this.updateFilters = updateFilters;
    this.setTimerange = setTimerange;
    this.setQuery = setQuery;
    this.getQuery = getQuery;
    this.setAggregationsEnabled = setAggregationsEnabled;
    this.setViewport = setViewport;
    this.loadNextPage = loadNextPage;
  };

  return Search;

});
