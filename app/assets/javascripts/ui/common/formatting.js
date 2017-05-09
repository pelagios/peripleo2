define([], function() {

  return {

    formatNumber : function(n) {
      return numeral(n).format('0,0');
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
