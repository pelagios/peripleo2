define([], function() {

  var MONTH_NAMES_SHORT = [
    'Jan', 'Feb', 'Mar', 'Apr',
    'May', 'Jun', 'Jul', 'Aug',
    'Sept', 'Oct', 'Nov', 'Dec' ];

  return {

    formatNumber : function(n) {
      return numeral(n).format('0,0');
    },

    formatDate : function(date) {
      var d = (date instanceof Date) ? date : new Date(Date.parse(date)),
          day = d.getDate(),
          month = d.getMonth(),
          year = d.getFullYear();

      return MONTH_NAMES_SHORT[month] + ' ' + day + ', ' + year;
    },

    formatYear : function(dateOrYear) {
      var year = (dateOrYear instanceof Date) ? dateOrYear.getFullYear() : parseInt(dateOrYear);
      if (year < 0) return -year + ' BC'; else return year + ' AD';
    },

    formatTemporalBounds : function(bounds) {
      if (bounds.from === bounds.to)
        return this.formatYear(bounds.from);
      else
        return this.formatYear(bounds.from) + ' - ' + this.formatYear(bounds.to);
    }

  };

});
