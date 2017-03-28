var repoLib = require('/lib/xp/repo');

exports.get = function (req) {

    var repoList = repoLib.list();

    return {
        contentType: 'application/json',
        body: {
            repoList: repoList
        }
    }
};