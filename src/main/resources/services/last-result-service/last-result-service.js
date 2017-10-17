var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var result = dataValidator.lastResult();

    return {
        contentType: 'application/json',
        body: {
            result: result
        }
    }
};

