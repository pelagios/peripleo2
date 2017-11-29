require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {

    var form = jQuery('.item-id'),
        input = form.find('input'),

        flashMessageBox = jQuery('.flash-message'),
        flashIcon = flashMessageBox.find('.icon'),
        flashMessage = flashMessageBox.find('.message'),

        code = jQuery('.json pre'),

        btnStore = jQuery('button.store'),
        btnCancel = jQuery('button.cancel'),

        showError = function(msg) {
          flashIcon.html('&#xf00d;');
          flashMessage.html(msg);
          flashMessageBox.attr('class', 'flash-message error');
          flashMessageBox.show();
        },

        showSuccess = function(msg) {
          flashIcon.html('&#xf00c;');
          flashMessage.html(msg);
          flashMessageBox.attr('class', 'flash-message success');
          flashMessageBox.show();
        },

        hideFlashMessage = function() {
          flashMessageBox.hide();
        },

        fetchItem = function(identifier) {
          jsRoutes.controllers.api.ItemAPIController.getItem(identifier)
            .ajax().done(function(item) {
              hideFlashMessage();
              code.html(JSON.stringify(item, null, 2));
            }).fail(function() {
              showError('Item &raquo;' + identifier + '&laquo; not found.');
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
          hideFlashMessage();
          code.empty();
        },

        onStoreJSON = function() {
          var json = code.html()
                .replace(/<br>/g, '')
                .replace(/<div>/g, '')
                .replace(/<\/div>/g, ''),
                
              // Minimal validation
              validationError = (function() {
                try {
                  JSON.parse(json);
                  return false;
                } catch (e) {
                  return e;
                }
                return false;
              })();

          if (validationError) {
            showError(validationError.message);
          } else {
            hideFlashMessage();
            jsRoutes.controllers.admin.maintenance.MaintenanceShaftController.updateItem().ajax({
              data: { item: json }
            }).done(function(response) {
              showSuccess('The item was updated.');
            }).fail(function(response) {
              showError('There was an error updating the item');
              console.log(response);
            });
          }
        };

    form.submit(onFindByIdentifier);
    btnStore.click(onStoreJSON);
    btnCancel.click(onCancel);
  });

});
