define([], function() {

  var TaskProgress = function(containerEl) {

      var element = jQuery(
            '<div class="task-progress">' +
              '<div class="meter">' +
                '<p class="label">0%</p>' +
                '<div class="bar rounded" style="width:0"></div>' +
              '</div>' +
            '</div>'),

          label = element.find('.label'),

          bar = element.find('.bar'),

          update = function(response) {
            var progress = response.progress + '%';
            label.html(progress);
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
