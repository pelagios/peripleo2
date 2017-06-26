define([
  'ui/common/formatting'
], function(Formatting) {

  var TEMPLATE = '<li class="meter">' +
                   '<div class="bar">' +
                     '<span class="count"></span>' +
                     '<span class="pad"></span>' +
                   '</div>' +
                   '<span class="label"></span>' +
                 '</li>';

  var FacetChart = function(parentEl) {

    var el = jQuery('<ul></ul>').appendTo(parentEl),

        createBar = function(count, label, percent) {
          var el = jQuery(TEMPLATE),
              countEl = el.find('.count'),
              barEl   = el.find('.bar'),
              labelEl = el.find('.label');

          countEl.html(Formatting.formatNumber(count));
          barEl.css('width', 0.6 * percent + '%');
          labelEl.html(label);

          return el;
        };

    el.append(createBar(1212, 'Graz University', 100));
    el.append(createBar(728, 'Nomisma Partner Objects', 72));
    el.append(createBar(312, 'American Numismatic Society', 34));
  };

  return FacetChart;

});
