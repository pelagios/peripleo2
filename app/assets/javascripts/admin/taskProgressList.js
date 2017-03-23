define([], function() {

  var TaskProgress = function(containerEl) {

      var element = jQuery(
            '<div class="task-progress">' +
              '<p class="caption"></p>' +
              '<div class="meter">' +
                '<p class="percentage">0%</p>' +
                '<div class="bar rounded" style="width:0"></div>' +
              '</div>' +
            '</div>'),

          caption = element.find('.caption'),
          percentage = element.find('.percentage'),
          bar = element.find('.bar'),

          update = function(response) {
            var progress = response.progress + '%';
            caption.html(response.caption);
            percentage.html(progress);
            bar.css({ width: progress });
          };

      containerEl.append(element);

      this.update = update;
    };

  var TaskProgressList = function(containerEl) {

    var progressBars = [],

        findBarById = function(taskId) {
          return progressBars.find(function(bar) {
            return bar.id === taskId;
          });
        },

        createBar = function(progress) {
          var bar = new TaskProgress(containerEl);
          bar.update(progress);
          progressBars.push({ id: progress.id, progress: bar });
        },

        updateProgress = function(response) {
          response.items.forEach(function(entry) {
            var bar = findBarById(entry.id);
            if (bar)
              bar.progress.update(entry);
            else
              createBar(entry);
          });
        };

    this.updateProgress = updateProgress;
  };

  return TaskProgressList;

});
