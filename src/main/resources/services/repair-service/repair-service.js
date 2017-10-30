var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var requestParams = req.params;

    var params = {
        nodeId: requestParams.nodeId,
        validatorName: requestParams.validatorName,
        repoId: requestParams.repoId,
        branch: requestParams.branch
    };

    var result = dataValidator.repair(params);

    return {
        contentType: 'application/json',
        body: {
            repairStatus: result
        }
    }
};
