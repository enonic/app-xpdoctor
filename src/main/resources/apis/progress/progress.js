var taskLib = require('/lib/xp/task');

exports.get = function (req) {
    var taskId = req.params.taskId;

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

    let info;
    try {
        info = JSON.parse(task.progress.info);
    } catch (e) {
        info = {
            validator: "?",
            repositoryId: "?",
            branch: "?"
        };
    }

    return {
        state: task.state,
        description: task.description,
        progress: {
            info: info,
            current: task.progress.current,
            total: task.progress.total
        }
    }
};

var errorMsg = function (msg) {

    return {
        contentType: 'application/json',
        body: {
            error: msg
        }
    }
};
