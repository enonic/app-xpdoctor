var dataValidator = require('/lib/dataValidator.js');

exports.post = function (req) {
    var params = JSON.parse(req.body);
    var issues = params['issues'];

    var validatorParams = {
        issues: issues
    };

    var revalidate = dataValidator.validateIds(validatorParams);

    return {
        contentType: 'application/json',
        body: {
            revalidate: revalidate
        }
    }
};

