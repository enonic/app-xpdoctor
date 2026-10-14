var taskLib = require('/lib/xp/task');


exports.get = function (req) {

    var task = getRunningTask();

    var model = {};

    if (!task) {
        model.state = "NOTASK"
    } else {
        model.state = task.state;
        model.taskId = task.id;
        model.type = task.description === app.name ? "analysis" : "repair";
    }

    return {
        contentType: 'application/json',
        body: model
    }
};

var getRunningTask = function () {

    var tasks = taskLib.list();

    for (var i = 0; i < tasks.length; i++) {

        if ((tasks[i].description === app.name || tasks[i].description === app.name + ".repair") &&
            (tasks[i].state === "RUNNING" || tasks[i].state === "WAITING")) {
            return tasks[i]
        }
    }

    return null;
};