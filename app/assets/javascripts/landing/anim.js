(function() {
  var runAnimations = function() {
        var sr = ScrollReveal();

        sr.reveal('.jumbotron .title', {
          origin   : 'top',
          distance : '15px',
          scale    : 1.35,
          duration : 600,
          easing   : 'ease-out' });

        sr.reveal('.jumbotron .enter', {
          origin   : 'bottom',
          distance : '40px',
          scale    : 1.0,
          easing   : 'ease-out',
          duration : 600 });

        sr.reveal('.livestats span', {
          origin   : 'bottom',
          distance : '20px',
          delay    : 700,
          scale    : 1.0}, 150);
      };

  runAnimations();

  jQuery(document).ready(function() {
    var doc = jQuery(document),
        header = jQuery('.header'),
        jumbotronHeight = jQuery('.jumbotron').outerHeight(),
        h = jumbotronHeight - header.outerHeight(),

        // TODO should ideally be pulled from original on the page, but we'll leave it hardwired for now
        r = 42, g = 57, b = 90,

        setOpacity = function() {
          var scrollTop = doc.scrollTop(),
              opacity = 1 - Math.max(0, (h - scrollTop)) / h,
              color = 'rgba(' + r + ',' + g + ',' + b + ',' + opacity + ')';

          header.css('backgroundColor', color);
        };

    doc.scroll(setOpacity);

    // Set on page load
    if (doc.scrollTop() > 0) setOpacity();
  });

})();
