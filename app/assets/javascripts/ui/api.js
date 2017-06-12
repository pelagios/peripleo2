define([], function() {

  return {

    getItem : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getItem(identifier).ajax();
    },

    getReferences : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getReferences(identifier).ajax();
    },

    getParts : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getParts(identifier).ajax();
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
      var url = '/api/search?limit=20&top_places=true&time_histogram=true&datasets=' + identifier;
      return jQuery.getJSON(url);
    }

  };

});
