require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {

    var form = jQuery('#item-id'),

        input = form.find('input'),

        code = jQuery('#json'),

        fetchItem = function(identifier) {
          jsRoutes.controllers.api.ItemAPIController.getItem(identifier)
            .ajax().done(function(item) {
              code.html(JSON.stringify(item, null, 2));
            });
        },

        onSubmit = function() {
          var identifier = input.val().trim();
          if (identifier.length > 0)
            fetchItem(identifier);
          return false;
        };

    form.submit(onSubmit);
  });

});
