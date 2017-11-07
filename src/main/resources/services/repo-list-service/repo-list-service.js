var repoLib = require('/lib/xp/repo');
var nodeLib = require('/lib/xp/node');

exports.get = function (req) {

    var repoList = repoLib.list();

    return {
        contentType: 'application/json',
        body: {
            repoList: createModel(repoList)
        }
    }
};

var createModel = function (repoList) {
    var entries = [];

    repoList.forEach(function (repo) {
        repo.branches.forEach(function (branch) {
            entries.push({
                repo: repo.id,
                branch: branch,
                count: getNodesInBranch(repo, branch)
            })
        });
    });

    return entries;
};


var getNodesInBranch = function (repo, branch) {
    var totalNodes = nodeLib.connect({
        repoId: repo.id,
        branch: branch
    }).query({
        start: 0,
        count: 0
    }).total;
    return totalNodes;
};