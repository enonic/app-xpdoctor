var bean = __.newBean('me.myklebust.xpdoctor.validator.IntegrityBean');


function required(params, name) {
    var value = params[name];
    if (value === undefined) {
        throw "Parameter '" + name + "' is required";
    }

    return value;
}

exports.validateAll = function (validatorParams) {
    var paramsObject = __.newBean('me.myklebust.xpdoctor.validator.ValidateParams');
    paramsObject.enabledValidators = validatorParams.enabledValidators;
    var result = bean.validate(paramsObject);
    return __.toNativeObject(result);
};

exports.validateIds = function (params) {
    var issueEntries = __.newBean('me.myklebust.xpdoctor.validator.model.IssueEntries');
    var issues = required(params, 'issues');
    issues.forEach(function (issue) {
        var entry = __.newBean('me.myklebust.xpdoctor.validator.model.IssueEntry');
        entry.nodeId = issue.nodeId;
        entry.repoId = issue.repoId;
        entry.branch = issue.branch;
        issueEntries.add(entry);
    });

    var result = bean.revalidate(issueEntries);
    return __.toNativeObject(result);
};


exports.lastResult = function () {
    var result = bean.getLastResult();
    return __.toNativeObject(result);
};


exports.validators = function () {
    var result = bean.validators();
    return __.toNativeObject(result);
};


exports.repair = function (params) {
    var paramsObject = __.newBean('me.myklebust.xpdoctor.validator.RepairParams');
    paramsObject.nodeId = required(params, "nodeId");
    paramsObject.validatorName = required(params, "validatorName");
    paramsObject.repoId = required(params, "repoId");
    paramsObject.branch = required(params, "branch");

    return __.toNativeObject(bean.repair(paramsObject));
};