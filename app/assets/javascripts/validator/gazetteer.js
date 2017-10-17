require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/hasUploads'], function(Uploads) {

  jQuery(document).ready(function() {

    var radioLabels = jQuery('input[name="format"] + label'),

        onSelectFormat = function(e) {
          var selectedLabel = jQuery('#' + this.id + ' + label');

          radioLabels.addClass('outline');
          selectedLabel.removeClass('outline');
        };

    jQuery('input[name="format"]').change(onSelectFormat);

    Uploads.enable('.upload');
  });

});
