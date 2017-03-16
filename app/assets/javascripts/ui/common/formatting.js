define([], function() {

  return {

    formatYear: function(dateOrYear) {
      var year = (dateOrYear instanceof Date) ? dateOrYear.getFullYear() : dateOrYear;
      if (year < 0) return -year + ' BC'; else return year + ' AD';
    }

  };

});
