require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/hasUploads'], function(Uploads) {

  jQuery(document).ready(function() {
    var btnRegisterVoID = jQuery('.register-void'),
        datasetList = jQuery('.dataset-list'),

        registerVoID = function() {
          var url = prompt('Enter VoID URL');
          if (url) {
            jsRoutes.controllers.admin.datasets.AnnotationsAdminController.importData()
              .ajax({ data: { 'url': url }});
          }
          return false;
        },

        deleteDataset = function(e) {
          var target = jQuery(e.target),
              btn = target.closest('.btn'),
              tr = target.closest('tr'),
              id = tr.data('id');
          jsRoutes.controllers.admin.datasets.AnnotationsAdminController.deleteDataset(id).ajax();
          btn.prop('disabled', true);
        },

        formatNumber = function() {
          var el = jQuery(this);
          el.html(numeral(el.html()).format('0,0'));
        };

    jQuery('.timeago').timeago();
    jQuery('.number').each(formatNumber);

    btnRegisterVoID.click(registerVoID);
    datasetList.on('click', '.btn.delete', deleteDataset);

    Uploads.enable('.upload');
  });

});
