var bean = __.newBean('me.myklebust.xpdoctor.validator.IntegrityBean');


function required(params, name) {
    var value = params[name];
    if (value === undefined) {
        throw "Parameter '" + name + "' is required";
    }

    return value;
}

exports.execute = function (validatorParams) {
    var paramsObject = __.newBean('me.myklebust.xpdoctor.validator.ValidateParams');
    paramsObject.setEnabledValidators(validatorParams.enabledValidators);

    for (const repoBranch of [].concat(validatorParams.repoBranches || [])) {
        // split the repoBranch string into repoId and branch
        var repoBranchItems = repoBranch.split(":");

        paramsObject.addRepoBranch(repoBranchItems[0], repoBranchItems[1]);
    }

    var result = bean.validate(paramsObject);
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
    paramsObject.setNodeId(required(params, "nodeId"));
    paramsObject.setValidatorName(required(params, "validatorName"));
    paramsObject.setRepoId(required(params, "repoId"));
    paramsObject.setBranch(required(params, "branch"));

    return __.toNativeObject(bean.repair(paramsObject));
};

