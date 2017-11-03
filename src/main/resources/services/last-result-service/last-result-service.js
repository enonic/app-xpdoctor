var dataValidator = require('/lib/dataValidator.js');
var issuesUtil = require('/lib/issues.js');

exports.get = function (req) {

    var result = dataValidator.lastResult();

    return {
        contentType: 'application/json',
        body: {
            result: issuesUtil.createIssuesModel(result)
        }
    }
};
