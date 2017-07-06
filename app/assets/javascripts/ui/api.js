define([], function() {

  return {

    getItem : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getItem(identifier).ajax();
    },

    getParts : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getParts(identifier).ajax();
    },

    getRelated : function(identifier, refId, query) {
      return jsRoutes.controllers.api.ItemAPIController.getRelated(identifier, refId, query).ajax();
    },

    getReferences : function(identifier, refId, query) {
      return jsRoutes.controllers.api.ItemAPIController.getReferences(identifier, refId, query).ajax();
    },

    suggest : function(query) {
      return jsRoutes.controllers.api.SearchAPIController.suggest(query).ajax();
    },

    /**
     * Bit of a hack, but might later be rolled into an official API method.
     * A transient query, completely bypassing the search state, with the following properties:
     *
     * - limit = 0
     * - top places = true
     * - time_histogram = true
     * - datasets = identifier
     */
    getDatasetInfo : function(identifier) {
      var url = '/api/search?limit=20&facets=true&top_places=true&time_histogram=true&datasets=' + identifier,

          // 'Fake' request args, so that this requests behaves just like a standard search
          // request made through application state
          requestArgs = {
            filters:   { datasets: [ identifier ] },
            timerange: { from: false, to : false },
            settings:  {
              timeHistogram   : true,
              termAggregations: true,
              topRelated      : true
            }
          };

      return jQuery.getJSON(url).then(function(response) {
        response.request_args = requestArgs;
        return response;
      });
    }

  };

});
