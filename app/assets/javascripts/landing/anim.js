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
})();
