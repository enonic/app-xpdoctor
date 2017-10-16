var dataValidator = require('/lib/dataValidator.js');

var eventLib = require('/lib/xp/event');
var webSocketLib = require('/lib/xp/websocket');

exports.get = function (req) {

    var taskId = dataValidator.execute();

    return {
        contentType: 'application/json',
        body: {
            taskId: taskId
        }
    }
};

