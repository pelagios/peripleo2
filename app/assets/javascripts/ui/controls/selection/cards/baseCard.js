define([
  'ui/common/formatting',
  'ui/common/itemUtils'
], function(Formatting, ItemUtils) {

  /** Contains common shorhands used in the selection box cards **/
  var BaseCard = function() {};

  /** Fills the element with the given HTML content **/
  BaseCard.prototype.fill = function(element, html) {
    element.html(html);
  };

  /** Like above, but handles cases where html is falsy **/
  BaseCard.prototype.fillIfExists = function(element, html) {
    if (html) element.html(html);
  };

  /** Fills the element with the first value in the array **/
  BaseCard.prototype.fillWithFirst = function(element, arr) {
    if (arr.length > 0) element.html(arr[0]);
  };

  BaseCard.prototype.renderIdentifiers = function(element, uris) {
    var identifiers = uris.map(function(uri) { return ItemUtils.parseEntityURI(uri); });
    identifiers.forEach(function(id) {
      var formatted = (id.shortcode) ? id.shortcode + ':' + id.id : id.uri,
          li = jQuery('<li><a href="' + id.uri + '" target="_blank">' + formatted + '</a></li>');

      if (id.color) li.css('background-color', id.color);
      element.append(li);
    });
  };

  /** Fills the element with the given temporalBounds value **/
  BaseCard.prototype.fillTemporalBounds = function(element, temporalBounds) {
    if (temporalBounds)
      element.html(Formatting.formatTemporalBounds(temporalBounds));
  };

  BaseCard.prototype.renderHierarchyPath = function(element, path) {
    if (path) {
      ItemUtils.getHierarchyPath(path).forEach(function(seg) {
        element.append(
          '<span>' +
            '<a class="destination" data-id="' + seg.id + '" href="#">' + seg.title + '</a>' +
          '</span>');
      });
    }
  };

  return BaseCard;

});
