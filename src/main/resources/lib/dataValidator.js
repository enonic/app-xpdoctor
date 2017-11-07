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
    paramsObject.enabledValidators = validatorParams.enabledValidators;
    paramsObject.repoId = required(validatorParams, "repoId");
    paramsObject.branch = required(validatorParams, "branch");
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
    paramsObject.nodeId = required(params, "nodeId");
    paramsObject.validatorName = required(params, "validatorName");
    paramsObject.repoId = required(params, "repoId");
    paramsObject.branch = required(params, "branch");

    return __.toNativeObject(bean.repair(paramsObject));
};

