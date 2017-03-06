require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {
    var btnRegisterVoID = jQuery('.register-void'),

        registerVoID = function() {
          var url = prompt('Enter VoID URL');
          if (url) {
            jsRoutes.controllers.admin.datasets.AnnotationsAdminController.importVoID()
              .ajax({ data: { 'url': url }});
          }
          return false;
        };

    btnRegisterVoID.click(registerVoID);
  });

});
