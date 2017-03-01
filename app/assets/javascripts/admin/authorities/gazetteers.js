require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['admin/taskProgressList'], function(TaskProgressList) {

  var POLL_INTERVAL_MS = 1000;

  var GazetteerOverview = function(containerEl, countEl) {

    var refresh = function(buckets) {
          var maxCount = Math.max.apply(null, jQuery.map(buckets, function(obj) {
                return obj[Object.keys(obj)[0]];
              })),

              toPercent = function(count) {
                return count / maxCount * 100;
              },

              createRow = function(gazetteer, count) {
                return jQuery(
                  '<tr>' +
                    '<td>' + gazetteer + '</a></td>' +
                    '<td>' +
                      '<div class="meter">' +
                        '<div class="bar rounded" style="width:' + toPercent(count) + '%"></div>' +
                      '</div>' +
                    '</td>' +
                    '<td>' + count + ' Records</td>' +
                  '</tr>');
              };

          containerEl.empty();
          countEl.html(buckets.length);

          buckets.forEach(function(obj) {
            var gazetteer = Object.keys(obj)[0],
                count = obj[gazetteer];

            containerEl.append(createRow(gazetteer, count));
          });
        };

    this.refresh = refresh;
  };

  jQuery(document).ready(function() {

    var progressList = new TaskProgressList(jQuery('.uploads')),

        gazetteerOverview =
          new GazetteerOverview(jQuery('.overview table'), jQuery('.gazetter-count')),

        queryProgress = function() {
          var retriesLeft = 3,

              onProgress = function(response) {
                progressList.updateProgress(response);
                window.setTimeout(queryProgress, POLL_INTERVAL_MS);
              },

              onFail = function(error) {
                if (retriesLeft > 0) {
                  retriesLeft -= 1;
                  window.setTimeout(query, POLL_INTERVAL_MS);
                } else {
                  // TODO UI error indication
                  console.log(error.responseText);
                }
              },

              query = function() {
                jsRoutes.controllers.api.admin.TaskAPIController.list()
                  .ajax({ data: { 'task_type': 'GAZETTEER_IMPORT' }})
                  .done(onProgress)
                  .fail(onFail);
              };

          query();
        },

        refreshOverview = function() {
          var onDone = function(response) {
                var buckets = response.aggregations.find(function(agg) {
                  return agg.name === 'by_dataset';
                }).buckets;

                gazetteerOverview.refresh(buckets);
                window.setTimeout(refreshOverview, POLL_INTERVAL_MS);
              },

              onFail = function(error) {
                // TODO UI error indication
                console.log(error.responseText);
              };

          jsRoutes.controllers.api.GazetteerAPIController.list().ajax()
            .done(onDone)
            .fail(onFail);
        };

    refreshOverview();
    queryProgress();
  });

});
