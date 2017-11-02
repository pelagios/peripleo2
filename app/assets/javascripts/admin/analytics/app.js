require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {

    var allTabs = jQuery('.tab-header .tab'),

        allTabContents = jQuery('.tab-content'),

        initTabbedViews = function() {

          jQuery('.tab-header .tab').click(function() {
            var tab = jQuery(this).closest('.tab'),
                targetId = tab.data('content'),
                target = jQuery('#' + targetId);

            allTabs.removeClass('active');
            allTabContents.hide();

            tab.addClass('active');
            target.show();
          });
        };

    initTabbedViews();
  });

});
