var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var params = req.params;
    var enabledValidators = params['enabledValidators[]'];

    var validatorParams = {
        enabledValidators: enabledValidators
    };

    var taskId = dataValidator.execute(validatorParams);

    return {
        contentType: 'application/json',
        body: {
            taskId: taskId
        }
    }
};

