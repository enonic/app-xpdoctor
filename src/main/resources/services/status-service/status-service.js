var taskLib = require('/lib/xp/task');
var mustache = require('/lib/xp/mustache');

exports.get = function (req) {
    var view = resolve('taskStatus.html');

    var taskId = getTaskId(req);

    if (!taskId) {
        return {
            contentType: 'text/html',
            body: "<div><p>No running jobs</p>"
        }
    }

    var task = taskLib.get(taskId);

    if (!task) {
        return errorMsg("Task with id [" + taskId + "] not found ");
    }

    return {
        contentType: 'text/html',
        body: mustache.render(view, task)
    }
};


var getTaskId = function (req) {
    var taskId = req.params.taskId;

    if (!taskId) {

        var tasks = taskLib.list();

        for (var i = 0; i < tasks.length; i++) {
            if (tasks[i].description === "com.enonic.app.repocleaner" && tasks[i].state === "RUNNING") {
                return tasks[i].id
            }
        }
    }

    return taskId;
};

var errorMsg = function (msg) {

    return {
        contentType: 'application/json',
        body: {
            error: msg
        }
    }
};