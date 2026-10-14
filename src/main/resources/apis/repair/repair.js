var dataValidator = require('/lib/dataValidator.js');

exports.post = function (req) {

    var params = req.params;

    var taskId;
    try {
        taskId = dataValidator.repairAll({
            all: params.all === 'true',
            issueIds: params['issueIds[]'] || params.issueIds
        });
    } catch (e) {
        return {
            contentType: 'application/json',
            body: {
                error: String(e)
            }
        }
    }

    return {
        contentType: 'application/json',
        body: {
            taskId: taskId
        }
    }
};
