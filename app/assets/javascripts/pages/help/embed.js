require([], function() {

  jQuery(document).ready(function() {
    var form     = jQuery('form.create-embed'),
        input    = form.find('input'),

        embedURI = jQuery('code .uri'),

        preview  = jQuery('.preview-container'),

        /** Shorthand **/
        fetchItem = function(url) {
          return jsRoutes.controllers.api.ItemAPIController.getItem(url).ajax();
        },

        onSubmit = function() {
          var pushState = function(url) {
                var pageUrl = window.location.protocol + '//' +
                      window.location.host +
                      window.location.pathname + '?url=' + encodeURIComponent(url);

                window.history.pushState({ path: pageUrl }, '', pageUrl);
              },

              itemURL = input.val();

          fetchItem(itemURL).done(function(item) {
            var id = item.is_conflation_of[0].uri,
                embedURL = jsRoutes.controllers.ApplicationController.embed(id).absoluteURL();

            embedURI.html(embedURL);
            renderPreview(embedURL);
            pushState(id);
          }).fail(function() {
            console.log('Not Found: ' + itemURL);
          });

          return false;
        },

        renderPreview = function(url) {
          preview.empty();
          jQuery('<iframe src="' + url + '" allowfullscreen="true"></iframe>').appendTo(preview);
        };

    form.submit(onSubmit);
  });

});
