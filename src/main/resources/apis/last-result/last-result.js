var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var result = dataValidator.lastResult();

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

    var issues = {};
    var summary = [];

    var issueNum = 0;

    result.repositories.forEach(function (repo) {
        repo.branches.forEach(function (branch) {
            summary.push(createSummary(repo, branch));
            branch.results.forEach(function (entry) {
                var issueId = 'issue-' + ++issueNum;
                issues[issueId] = createIssue(repo, branch, entry, issueId);
            });
        });
    });

    return {
        totalIssues: result.totalIssues,
        timestamp: result.timestamp,
        summary: summary,
        issues: issues
    }
};

var createSummary = function (repo, branch) {
    return {
        repo: repo.id,
        branch: branch.branch,
        analysed: branch.analysed,
        invalid: branch.invalidNodes,
        repairable: branch.repairableNodes
    }
};

var createIssue = function (repo, branch, entry, issueId) {
    return {
        issueId: issueId,
        repo: repo.id,
        branch: branch.branch,
        type: entry.type,
        nodeId: entry.id,
        nodeVersionId: entry.nodeVersionId,
        path: entry.path,
        message: entry.message,
        validatorName: entry.validatorName,
        repairStatus: entry.repair.status,
        repairMessage: entry.repair.message
    }
};

