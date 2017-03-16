define([], function() {

  /** Flag that indicates wether the device supports touch events **/
  var hasTouch = ('ontouchstart' in window) || (navigator.MaxTouchPoints > 0),

      // Cf. http://stackoverflow.com/questions/5186441/javascript-drag-and-drop-for-touch-devices
      touchHandler = function(event) {
        var touch = event.changedTouches[0],
            simulatedEvent = document.createEvent('MouseEvent'),
            type;

        // Translate event types
        if (event.type === 'touchstart')
          type = 'mousedown';
        else if (event.type === 'touchmove')
          type = 'mousemove';
        else if ( event.type === 'touchend')
          type = 'mouseup';
        else
          // break
          return;

        simulatedEvent.initMouseEvent(type, true, true, window, 1,
          touch.screenX, touch.screenY, touch.clientX, touch.clientY,
          false, false, false, false, 0, null);

        touch.target.dispatchEvent(simulatedEvent);
        event.preventDefault();
      };

  return {

    makeXDraggable: function(element, onDrag, onStop, opt_containment) {
      if (hasTouch) {
        element[0].addEventListener('touchstart', touchHandler, true);
        element[0].addEventListener('touchmove', touchHandler, true);
        element[0].addEventListener('touchend', touchHandler, true);
        element[0].addEventListener('touchcancel', touchHandler, true);
      }

      element.draggable({
        axis: 'x',
        containment: opt_containment,
        drag: onDrag,
        stop: onStop
      });
    }

  };

});
