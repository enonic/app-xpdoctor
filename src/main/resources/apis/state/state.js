var taskLib = require('/lib/xp/task');


exports.get = function (req) {

    var taskId = getRunningTask();

    var model = {};

    if (!taskId) {
        model.state = "NOTASK"
    } else {
        model.state = "RUNNING";
        model.taskId = taskId;
    }

    return {
        contentType: 'application/json',
        body: model
    }
};

var getRunningTask = function () {

    var tasks = taskLib.list();

    for (var i = 0; i < tasks.length; i++) {

        if (tasks[i].description === app.name && tasks[i].state === "RUNNING") {
            return tasks[i].id
        }
    }

    return null;
};