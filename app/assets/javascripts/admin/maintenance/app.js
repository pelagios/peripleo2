require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {

    var form = jQuery('.item-id'),
        input = form.find('input'),

        errorBox = jQuery('.error'),
        errorMsg = errorBox.find('.message'),

        code = jQuery('.json pre'),

        btnStore = jQuery('button.store'),
        btnCancel = jQuery('button.cancel'),

        showError = function(msg) {
          errorMsg.html(msg);
          errorBox.show();
        },

        hideError = function() {
          errorBox.hide();
          errorMsg.empty();
        },

        fetchItem = function(identifier) {
          jsRoutes.controllers.api.ItemAPIController.getItem(identifier)
            .ajax().done(function(item) {
              hideError();
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
          hideError();
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
            showError(validationError.message);
          } else {
            hideError();
            jsRoutes.controllers.admin.maintenance.MaintenanceShaftController.updateItem().ajax({
              data: { item: json }
            }).done(function(response) {
              console.log('done');
              console.log(response);
            });
          }
        };

    form.submit(onFindByIdentifier);
    btnStore.click(onStoreJSON);
    btnCancel.click(onCancel);
  });

});
