var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var result = dataValidator.lastResult();

    log.info("LastResultService result: %s", JSON.stringify(result));


    return {
        contentType: 'application/json',
        body: {
            result: createIssuesModel(result)
        }
    }
};


var createIssuesModel = function (result) {

    if (!result) {
        return {};
    }

    var issues = [];

    result.repositories.forEach(function (repo) {
        repo.branches.forEach(function (branch) {
            branch.results.forEach(function (entry) {
                issues.push(createIssue(repo, branch, entry));
            });
        });
    });

    return {
        totalIssues: result.totalIssues,
        timestamp: result.timestamp,
        issues: issues
    }
};


var createIssue = function (repo, branch, entry) {

    return {
        repo: repo.id,
        branch: branch.branch,
        type: entry.type,
        id: entry.id,
        path: entry.path,
        message: entry.message,
        validatorName: entry.validatorName,
        repairStatus: entry.repair.status,
        repairMessage: entry.repair.message
    }


};