var webSocketLib = require('/lib/xp/websocket');
var eventLib = require('/lib/xp/event');
var channel = 'result';

var listenerAdded = false;

function handleGet(req) {

    if (!listenerAdded) {

        eventLib.listener({
            type: app.name + ".*",
            localOnly: false,
            callback: function (event) {

                log.info("(event-bus) EVENT: %s", JSON.stringify(event));

                sendToGroup(channel, {type: 'jobFinished', data: event.data.result});
            }
        });

        listenerAdded = true;
    }

    if (!req.webSocket) {
        return {
            status: 404
        };
    }

    return {
        webSocket: {
            data: {},
            subProtocols: ["result"]
        }
    };


}

function sendToGroup(channel, message) {
    var msg = JSON.stringify(message);
    webSocketLib.sendToGroup(channel, msg);
}

function handleEvent(event) {

    if (event.type === 'open') {
        connect(event);
    }

    if (event.type === 'message') {
        handleWsMessage(event);
    }

    if (event.type === 'close') {
        leave(event);
    }
}

function connect(event) {
    webSocketLib.addToGroup(channel, getSessionId(event));
}

function handleWsMessage(event) {
    var message = JSON.parse(event.message);

    /* if (message.action == 'join') {
         join(event, message.avatar);
         return;
     }

     if (message.action == 'chatMessage') {
         if (message.message.toLowerCase() == 'info') {
             sendInfoMessage(event);
         }
         else {
             handleChatMessage(event, message);
         }
         return;
     }
     */
}

function leave(event) {
    var sessionId = getSessionId(event);
    websocketLib.removeFromGroup(chatGroup, sessionId);
}

function getSessionId(event) {
    return event.session.id;
}

exports.webSocketEvent = handleEvent;
exports.get = handleGet;