define([], function() {

  return {

    getItem : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getItem(identifier).ajax();
    },

    getParts : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getParts(identifier).ajax();
    },

    getReferences : function(identifier, refId, query) {
      return jsRoutes.controllers.api.ItemAPIController.getReferences(identifier, refId, query).ajax();
    },

    getTopReferenced : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getTopReferenced(identifier).ajax();
    },

    suggest : function(query) {
      return jsRoutes.controllers.api.SearchAPIController.suggest(query).ajax();
    },

    /**
     * Bit of a hack, but we might later roll it into an official API method. It's a transient
     * query, completely bypassing the search state, with the following properties:
     * - limit = default
     * - top_referenced = true
     * - facets         = true
     * - time_histogram = true
     * - datasets       = {identifier}
     */
    getDatasetInfo : function(identifier) {
      var url = '/api/search?facets=true&top_referenced=true&time_histogram=true&datasets=' + identifier,

          /**
           * Reproduces the follow-up functionality that's also included in the
           * main search.js component.
           */
          fetchMore = function(response, n) {
            var nextPageURL = '/api/search?limit=' + n + '&datasets=' + identifier;
            return jQuery.getJSON(nextPageURL).then(function(nextPage) {
              response.items = response.items.concat(nextPage.items);
              return response;
            });
          },

          requestArgs = {
            filters:   { datasets: [ identifier ] },
            timerange: { from: false, to : false },
            settings:  {
              timeHistogram    : true,
              termAggregations : true,
              topReferenced    : true
            }
          };

      return jQuery.getJSON(url).then(function(response) {
        response.request_args = requestArgs;
        if (response.top_referenced.PLACE)
          return response;
        else
          return fetchMore(response, 600);
      });
    }

  };

});
