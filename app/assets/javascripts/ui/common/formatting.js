define([], function() {

  var MONTH_NAMES_SHORT = [
    'Jan', 'Feb', 'Mar', 'Apr',
    'May', 'Jun', 'Jul', 'Aug',
    'Sept', 'Oct', 'Nov', 'Dec' ],

     PATH_SEPARATOR = '\u0007';

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
    },

    formatClickableURL : function(url) {
      if (url.indexOf('http') === 0) {
        var withoutProtocol = url.substring(url.indexOf(':') + 3),
            label = (withoutProtocol.indexOf('/') > -1) ?
              withoutProtocol.substring(0, withoutProtocol.indexOf('/')) :
              withoutProtocol;

        return '<a href="' + url + '" target="_blank">' + label  + '</a>';
      } else {
        return url;
      }
    },

    /** Shorthand to fetch labels from a path **/
    formatPath : function(path) {
      return path.map(function(segment) { return segment.label; })
        .join('\u0007').replace('\u0007', '<span class="path-separator"></span>');
    }

  };

});
