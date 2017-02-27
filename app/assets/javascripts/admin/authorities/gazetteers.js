require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['admin/taskProgressList'], function(TaskProgressList) {

  var POLL_INTERVAL_MS = 1000;

  jQuery(document).ready(function() {

    var progressList = new TaskProgressList(jQuery('.current-uploads')),

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
                  new Alert(Alert.ERROR, 'Error', error.responseText);
                }
              },

              query = function() {
                jsRoutes.controllers.api.admin.TaskAPIController.list()
                  .ajax({ data: { 'task_type': 'GAZETTEER_IMPORT' }})
                  .done(onProgress)
                  .fail(onFail);
              };

          query();
        };

    queryProgress();
  });

});
