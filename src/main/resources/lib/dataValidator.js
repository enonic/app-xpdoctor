var bean = __.newBean('me.myklebust.xpdoctor.validator.IntegrityBean');

exports.execute = function () {
    var result = bean.validate();
    return __.toNativeObject(result);
};


exports.validators = function () {
    var result = bean.validators();
    return __.toNativeObject(result);
};
