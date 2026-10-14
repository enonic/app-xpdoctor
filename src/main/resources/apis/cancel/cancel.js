var dataValidator = require('/lib/dataValidator.js');

exports.post = function (req) {

    dataValidator.cancel();

    return {
        contentType: 'application/json',
        body: {
            cancelled: true
        }
    }
};
