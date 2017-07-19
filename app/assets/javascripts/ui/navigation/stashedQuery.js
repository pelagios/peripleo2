define([], function() {

  /**
   * A helper so we can share the stashed query.
   */
  var StashedQuery = function() {
    this.query = false;
  };

  StashedQuery.prototype.clear = function() {
    this.query = false;
  };

  StashedQuery.prototype.set = function(q) {
    this.query = q;
  };

  StashedQuery.prototype.get = function() {
    return this.query;
  };

  StashedQuery.prototype.isSet = function() {
    return this.query !== false;
  };

  return StashedQuery;

});
