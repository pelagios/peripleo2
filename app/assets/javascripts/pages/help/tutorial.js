require([], function() {

  jQuery(document).ready(function() {
    var element = jQuery('.sidebar'),
        maxScroll = jQuery('.top').outerHeight(),

        makeElementSticky = function(element, maxScroll) {
          var onScroll = function() {
            var scrollTop = jQuery(window).scrollTop();
            if (scrollTop > maxScroll)
              element.addClass('fixed');
            else
              element.removeClass('fixed');
          };

          // In case the page is initally scrolled after load
          onScroll();
          jQuery(window).scroll(onScroll);
        },

        animateAnchorNav = function(containerEl) {
          containerEl.on('click', 'a', function(e) {
            var ul = jQuery(e.target).closest('ul'),
                isSubmenu = ul.hasClass('sub-topics');
                
            if (isSubmenu) {
              e.preventDefault();

              jQuery('html, body').animate({
                scrollTop: jQuery(jQuery.attr(this, 'href')).offset().top
              }, 500);

              return false;
            } // Otherwise, just follow the link
          });
        };

    makeElementSticky(element, maxScroll);
    animateAnchorNav(element.find('ul.help-topics'));
  });

});
