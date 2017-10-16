var model = {
    selectors: {},
    buttons: {
        runValidator: "#btnRunValidator",
        resultTabActivator: ".resultTabActivator"
    },
    div: {
        status: "#progressReporter",
        results: "#results",
        resultTab: "#resultTab",
        mainTab: "#mainTab"
    }
};


var ws = {
    connected: false,
    connection: null,
    keepAliveIntervalId: null
};

var state = {
    taskId: null,
    taskState: null
};


$(function () {

    wsConnect();
    getState();
    getStatus();

    $(model.buttons.runValidator).click(function () {
        doValidation();
        getStatus();
        getState();
    });

    setInterval(getStatus, 1000);
    setInterval(getState, 1000);
});


function wsConnect() {
    console.log("Connecting to WS");
    ws.connection = new WebSocket(wsUrl, ['result']);
    ws.connection.onopen = onWsOpen;
    ws.connection.onclose = onWsClose;
    ws.connection.onmessage = onWsMessage;
}


function onWsOpen() {

    console.log("Connected to WS");

    ws.keepAliveIntervalId = setInterval(function () {
        if (ws.connected) {
            this.ws.connection.send('{"action":"KeepAlive"}');
        }
    }, 30 * 1000);
    ws.connected = true;
}

function onWsClose() {
    clearInterval(keepAliveIntervalId);
    ws.connected = false;

    setTimeout(wsConnect, 2000); // attempt to reconnect
}

function onWsMessage(event) {
    var message = JSON.parse(event.data);

    console.log("Message received: ", message);

    if (message.type === "jobFinished") {
        handleJobFinished(message);
    }
}

var handleJobFinished = function (message) {
    var results = JSON.parse(message.data);
    $(model.div.resultTab).html(renderResult(results));
};

var renderResult = function (results) {

    console.log("REPORISOTREI: ", results.repositories);

    var html = "";
    results.repositories.forEach(function (repo) {

        html += "<h2>" + repo + "</h2>";


    });

    return html;
};

var doValidation = function () {
    jQuery.ajax({
        url: validatorServiceUrl,
        cache: false,
        type: 'GET',
        success: function (result) {
            console.log("VALIDATION: ", result);
        }
    });
};

var getStatus = function () {

    var data = {
        taskId: state.taskId
    };

    jQuery.ajax({
        url: statusServiceUrl,
        data: data,
        cache: false,
        type: 'GET',
        success: function (result) {
            $(model.div.status).html(result);
        }
    });

};


var getState = function () {

    jQuery.ajax({
        url: stateServiceUrl,
        cache: false,
        type: 'GET',
        success: function (result) {

            $(model.div.status).show();

            if (result.state === "RUNNING") {
                state.taskId = result.taskId;
                $(model.buttons.runValidator).prop("disabled", true);
                $(model.buttons.runValidator + " > i").html("update");
                $(model.buttons.runValidator + " > span").html("Running");
                toggleState($(model.div.status), "ok");
            } else {
                if (result.state === "FAILED") {
                    toggleState($(model.div.status), "error");
                }
                $(model.buttons.runValidator).prop("disabled", false);
                $(model.buttons.runValidator + " > i").html("play_arrow");
                $(model.buttons.runValidator + " > span").html("Start");
            }
        }
    });
};

var toggleState = function (element, state) {
    element.removeClass("ok", "error");
    element.addClass(state);
};