var dataValidator = require('/lib/dataValidator.js');

exports.post = function (req) {

    var params = req.params;
    var enabledValidators = params['enabledValidators[]'];
    var repoBranches = params['repoBranches[]'];

    var validatorParams = {
        enabledValidators: enabledValidators,
        repoBranches: repoBranches,
    };

    var taskId;
    try {
        taskId = dataValidator.execute(validatorParams);
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

