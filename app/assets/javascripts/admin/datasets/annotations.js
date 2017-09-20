require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['admin/hasUploads'], function(Uploads) {

  jQuery(document).ready(function() {
    var btnRegisterVoID = jQuery('.register-void'),

        registerVoID = function() {
          var url = prompt('Enter VoID URL');
          if (url) {
            jsRoutes.controllers.admin.datasets.AnnotationsAdminController.importData()
              .ajax({ data: { 'url': url }});
          }
          return false;
        };

    jQuery('.timeago').timeago();

    btnRegisterVoID.click(registerVoID);
    Uploads.enable('.new-upload');
  });

});
