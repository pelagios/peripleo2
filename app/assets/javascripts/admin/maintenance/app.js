require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {

    var form = jQuery('.item-id'),
        input = form.find('input'),

        errorMsg = jQuery('.error-message'),

        code = jQuery('.json pre'),

        btnStore = jQuery('button.store'),
        btnCancel = jQuery('button.cancel'),

        fetchItem = function(identifier) {
          jsRoutes.controllers.api.ItemAPIController.getItem(identifier)
            .ajax().done(function(item) {
              code.html(JSON.stringify(item, null, 2));
            });
        },

        onFindByIdentifier = function() {
          var identifier = input.val().trim();
          if (identifier.length > 0)
            fetchItem(identifier);
          return false;
        },

        onCancel = function() {
          input.val('');
          errorMsg.hide();
          errorMsg.empty();
          code.empty();
        },

        onStoreJSON = function() {
          var json = code.html(),

              // Minimal validation
              validationError = (function() {
                try {
                  JSON.parse(json);
                  return false;
                } catch (e) {
                  return e;
                }
              })();

          if (validationError) {
            errorMsg.html(validationError.message);
            errorMsg.show();

            // TODO show error message
          } else {
            errorMsg.hide();
            errorMsg.empty();

            // TODO post item back to index
          }
        };

    form.submit(onFindByIdentifier);
    btnStore.click(onStoreJSON);
    btnCancel.click(onCancel);
  });

});
