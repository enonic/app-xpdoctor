var bean = __.newBean('me.myklebust.xpdoctor.validator.IntegrityBean');


exports.execute = function (repositoryId, branch) {
    var result = bean.execute(repositoryId, branch);
    return __.toNativeObject(result);
};


