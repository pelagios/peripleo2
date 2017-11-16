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

        // TODO should ideally be pulled from the CSS/page, but we'll leave it hardwired for now
        r = 42, g = 57, b = 90;

    doc.scroll(function() {
      var scrollTop = doc.scrollTop(),
          opacity = 1 - Math.max(0, (jumbotronHeight - scrollTop)) / jumbotronHeight,
          color = 'rgba(' + r + ',' + g + ',' + b + ',' + opacity + ')';

      console.log(color);
      header.css('backgroundColor', color);

      // header.css('backgroundColor', 'rgba(' + r + ',' + g + ',' + b + ',' + opacity + ');');
    });
  });

})();
