define([
  'ui/navigation/selecting/item/baseActions',
  'ui/api'
], function(BaseActions, API) {

  var ObjectActions = function(map, searchPanel, selectionPanel, resultList, state, stashedQuery) {

    var self = this,

        /**
         * Fetches the number of search results linked to the referenced item, so that we can,
         * e.g., display information like "Athens - 2.232 more results".
         *
         * Note: this function takes into account the current search state, so all currently set
         * filters, the time range restriction, the query, etc. influence the result.
         */
        fetchResultsAtReferenced = function(uri, opt_prev) {
              // So we can chain results
          var previous = opt_prev || [],
              filter = { referencing : [ uri ] };

          return state.updateFilters(filter, { pushState: false })
            .then(function(results) {
              state.updateFilters({ referencing: false }, self.NOOP);
              previous.push({ 'identifier' : uri, 'resultCount' : results.total });
              return previous;
            });
        },

        /**
         * Fetches:
         * 1. the top items referenced by the item with the specified URI
         * 2. for the first three items of each type, the result count (using
         *    the fetchResultsAtReferenced function)
         */
        fetchTopReferencedData = function(uri) {
          return API.getTopReferenced(uri).then(function(referenced) {
                // Get first three items for types Place, Person, Period
            var topPlaces  = (referenced.PLACE)  ? referenced.PLACE.slice(0, 3)  : [],
                topPeople  = (referenced.PERSON) ? referenced.PERSON.slice(0, 3) : [],
                topPeriods = (referenced.PERIOD) ? referenced.PERIOD.slice(0, 3) : [],

                topURIs = topPlaces.concat(topPeople, topPeriods).map(function(item) {
                  return item.is_conflation_of[0].identifiers[0];
                });

            // Chain promises, so that they run in sequence
            return topURIs.reduce(function(p, uri) {
              return p.then(function(result) {
                return fetchResultsAtReferenced(uri, result);
              });
            }, jQuery.Deferred().resolve().promise()).then(function(results) {
              return { referenced: referenced, referenceCounts: results };
            });
          });
        },

        select = function(item, opt_via) {
          var via = opt_via && jQuery.isPlainObject(opt_via);

          return fetchTopReferencedData(item.is_conflation_of[0].identifiers[0]).done(function(results) {
            state.setSelectedItem(item);
            resultList.setSelectedItem(item);

            // Reminder: selection panel uses via for ref list & query phrase for text snippets
            selectionPanel.show(item, jQuery.extend({}, results, {
              selected_via : via,
              query_phrase : state.getQueryPhrase()
            }));

            searchPanel.setLoading(false);

            if (!via) map.setSelectedItem(item, results.referenced.PLACE);
          });
        };

    this.select = select;

    BaseActions.apply(this, [ selectionPanel, resultList, state ]);
  };
  ObjectActions.prototype = Object.create(BaseActions.prototype);

  return ObjectActions;

});
