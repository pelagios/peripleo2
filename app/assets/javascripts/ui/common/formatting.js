define([], function() {

  return {

    formatNumber: function(n) {
      return numeral(n).format('0,0');
    },

    formatYear: function(dateOrYear) {
      var year = (dateOrYear instanceof Date) ? dateOrYear.getFullYear() : dateOrYear;
      if (year < 0) return -year + ' BC'; else return year + ' AD';
    }

  };

});
