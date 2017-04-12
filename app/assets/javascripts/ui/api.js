define([], function() {

  return {

    getItem : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getItem(identifier).ajax();
    },

    getReferences : function(identifier) {
      return jsRoutes.controllers.api.ItemAPIController.getReferences(identifier).ajax();
    },

    suggest : function(query) {
      return jsRoutes.controllers.api.SearchAPIController.suggest(query).ajax();
    }

  };

});
