define([], function() {

      /** Number of results per page **/
  var PAGE_SIZE = 20,

      // To throttle traffic, we'll stay idle between requests for this time in millis
      IDLE_MS = 200;

  var Search = function() {

    var self = this,

        searchArgs = {

          query: false,

          filters: {}, // key -> [ value, value, value, ...]

          timerange: { from: false, to : false },

          // TODO just a hack for now
          bbox: false,

          settings: {

            timeHistogram : false,

            termAggregations: false,

            topPlaces: true

          }

        },

        currentOffset = 0,

        // Are we currently waiting for an API response?
        busy = false,

        // Last pending request that arrived while busy
        pendingRequest = false,

        /** Returns (a clone of) the current search args **/
        getCurrentArgs = function() {
          return jQuery.extend({}, searchArgs);
        },

        // DRY helper
        appendIfExists = function(param, key, url) {
          if (param) return url + '&' + key + '=' + param;
          else return url;
        },

        buildBaseQuery = function() {
          var url = '/api/search?limit=' + PAGE_SIZE;

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

        buildFirstPageQuery = function(opt_settings) {
          var url = buildBaseQuery(),
              settings = (opt_settings) ?
               jQuery.extend({}, searchArgs.settings, opt_settings) :
               searchArgs.settings;

          // First page query includes aggregations
          url = appendIfExists(settings.timeHistogram, 'time_histogram', url);
          url = appendIfExists(settings.termAggregations, 'facets', url);
          url = appendIfExists(settings.topPlaces, 'top_places', url);

          // Reset offset for subsequent next page queries
          currentOffset = 0;

          return url;
        },

        buildNextPageQuery = function() {
          // TODO more to come later
          return buildBaseQuery() + '&offset=' + (currentOffset + PAGE_SIZE);
        },

        makeRequest = function(opt_settings) {

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
                jQuery.getJSON(buildFirstPageQuery(opt_settings), function(response) {
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

        loadNextPage = function() {
          var deferred = jQuery.Deferred();

          jQuery.getJSON(buildNextPageQuery(), function(response) {
            currentOffset = response.offset;
            deferred.resolve(response);
          });

          return deferred.promise(this);
        },

        set = function(args) {
          searchArgs = args;
          return makeRequest();
        },

        clear = function(makeReq) {
          searchArgs.query = false;
          searchArgs.filters = {};
          searchArgs.timerange = { from: false, to : false };

          if (makeReq)
            return makeRequest();
        },

        setQuery = function(query, makeReq) {
          searchArgs.query = query;
          if (makeReq)
            return makeRequest();
        },

        getQuery = function() {
          return searchArgs.query;
        },

        updateFilters = function(diff, makeReq) {
          jQuery.extend(searchArgs.filters, diff);
          if (makeReq)
            return makeRequest();
        },

        setTimerange = function(range) {
          // There's no need to re-compute the time histogram in case the time range changes
          searchArgs.timerange = range;
          return makeRequest({ timeHistogram: false });
        },

        updateSettings = function(diff) {
          jQuery.extend(searchArgs.settings, diff);
          return makeRequest();
        },

        setAggregationsEnabled = function(enabled) {
          return updateSettings({
            timeHistogram    : enabled,
            termAggregations : enabled,
          });
        },

        setViewport = function(bounds) {
          // TODO just a hack for now
          // searchArgs.bbox = bounds;
        };

    this.clear = clear;
    this.getCurrentArgs = getCurrentArgs;
    this.loadNextPage = loadNextPage;
    this.set = set;
    this.setAggregationsEnabled = setAggregationsEnabled;
    this.setQuery = setQuery;
    this.getQuery = getQuery;
    this.setTimerange = setTimerange;
    this.updateFilters = updateFilters;
    this.updateSettings = updateSettings;
    this.setViewport = setViewport;
  };

  return Search;

});
