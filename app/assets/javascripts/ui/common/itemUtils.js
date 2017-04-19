define(function() {

  return {

    /** We'll do this pre-processing step on the server later! **/
    getHierarchyPath : function(hierarchy) {
      var path = hierarchy.paths,
          last = path[path.length - 1],
          tuples = last.split('\u0007\u0007');

      return tuples.map(function(str) {
        var tuple = str.split('\u0007');
        return { 'id': tuple[0], 'title': tuple[1] };
      });
    },

    /** Shorthand to make handling of item types a bit easier **/
    getItemType : function(item) {
      var t = item.item_type;

      if (t.indexOf('PLACE') > -1) {
        return 'PLACE';
      } else if (t.indexOf('OBJECT') > -1) {
        return 'OBJECT';
      } else if (t.indexOf('PERSON') > -1) {
        return 'PERSON';
      } else if (t.indexOf('DATASET') > -1) {
        return 'DATASET';
      }
    }

  };

});
