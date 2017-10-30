var taskLib = require('/lib/xp/task');

exports.get = function (req) {

    var taskId = getTaskId(req);

    if (!taskId) {
        return {
            contentType: 'application/json',
            body: {
                state: 'not running'
            }
        }
    }

    var task = taskLib.get(taskId);

    if (!task) {
        return errorMsg("Task with id [" + taskId + "] not found ");
    }

    return {
        contentType: 'application/json',
        body: createModel(task)
    }
};

var createModel = function (task) {

    return {
        state: task.state,
        description: task.description,
        progress: {
            info: task.state === "RUNNING" ? JSON.parse(task.progress.info) : "done",
            current: task.progress.current,
            total: task.progress.total
        }
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