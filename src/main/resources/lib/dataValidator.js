var bean = __.newBean('me.myklebust.xpdoctor.validator.IntegrityBean');

exports.execute = function () {
    var result = bean.execute();
    return __.toNativeObject(result);
};


