var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var params = req.params;
    var enabledValidators = params['enabledValidators[]'];
    var repoId = params['repoId'];
    var branch = params['branch'];

    if (!repoId || !branch) {
        throw "Missing repo / branch selector value";
    }

    var validatorParams = {
        enabledValidators: enabledValidators,
        repoId: repoId,
        branch: branch
    };

    var taskId = dataValidator.execute(validatorParams);

    return {
        contentType: 'application/json',
        body: {
            taskId: taskId
        }
    }
};

