define([
  'ui/common/hasEvents',
  'ui/map/baselayers'
], function(HasEvents, BaseLayers) {

  var BaseLayerSwitcher = function() {

    var self = this,

        element = jQuery(
          '<div class="clicktrap">' +
            '<div class="modal-wrapper ls-wrapper">' +
              '<div class="modal layerswitcher">' +
                '<div class="modal-header ls-header">' +
                  '<h2>Select Base Map</h2>' +
                  '<button class="icon tonicons close">&#xe897;</button>' +
                '</div>' +
                '<div class="modal-body ls-body">' +
                  '<ul></ul>' +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>').hide().appendTo(document.body),

        list = element.find('ul'),

        init = function() {
          var switcher = element.find('.layerswitcher'),
              handle   = element.find('.ls-header'),
              btnClose = element.find('.close'),

              renderRow = function(layer) {
                jQuery('<li data-name="' + layer.id + '">' +
                  '<div class="thumb-container"><img class="map-thumb" src="' + layer.thumb_url + '"></div>' +
                  '<h3>' + layer.title + '</h3>' +
                  '<p>' + layer.description + '</p>' +
                '</li>').appendTo(list);
              },

              onSelect = function(e) {
                var target = jQuery(e.target),
                    a = target.closest('a'),
                    li = target.closest('li'),
                    layerName = li.data('name');

                // Don't trigger select if the click was on a link
                if (a.length === 0) {
                  self.fireEvent('changeBasemap', layerName);
                  close();
                }
              };

          BaseLayers.all().forEach(renderRow);
          switcher.draggable({ handle: handle });
          element.on('click', 'li', onSelect);
          btnClose.click(close);
        },

        open = function() {
          element.show();
        },

        close = function() {
          element.hide();
        };

    init();
    this.open = open;
    HasEvents.apply(this);
  };
  BaseLayerSwitcher.prototype = Object.create(HasEvents.prototype);

  return BaseLayerSwitcher;

});
