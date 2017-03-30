require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'admin/hasUploads'
], function(Uploads) {

  jQuery(document).ready(function() {

    // TODO

    Uploads.enable('.new-upload');
  });

});
