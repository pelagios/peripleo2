require([], function() {

  jQuery(document).ready(function() {

    var form     = jQuery('form.create-embed'),
        input    = form.find('input'),

        embedURI = jQuery('code .uri'),

        preview  = jQuery('.preview-container'),

        onSubmit = function() {
          var pushState = function(url) {
                var pageUrl = window.location.protocol + '//' +
                      window.location.host +
                      window.location.pathname + '?url=' + url;

                window.history.pushState({ path: pageUrl }, '', pageUrl);
              },

              itemURI = input.val(),
              url = jsRoutes.controllers.ApplicationController.embed(itemURI).absoluteURL();

          embedURI.html(url);
          renderPreview(url);
          pushState(itemURI);
          return false;
        },

        renderPreview = function(url) {
          preview.empty();
          jQuery('<iframe src="' + url + '" allowfullscreen="true"></iframe>').appendTo(preview);
        };

    form.submit(onSubmit);
  });

});
