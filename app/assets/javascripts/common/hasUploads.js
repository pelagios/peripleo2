define(function() {

  return {

    enable : function(selector) {
      var doc = jQuery(document),

          forwardClick = function(e) {
            var inputId = e.target.getAttribute('data-input');
            jQuery('#' + inputId).click();
          },

          submitImmediately = function(e) {
            e.target.parentNode.submit();
          };

      doc.on('click', selector, forwardClick);
      doc.on('change', 'input:file', submitImmediately);
    }

  };

});
