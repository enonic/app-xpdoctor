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
    taskState: null,
    resultTimestamp: 0
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
    setInterval(getLastResult, 3000);
});

var getLastResult = function () {

    jQuery.ajax({
        url: lastResultServiceUrl,
        cache: false,
        type: 'GET',
        success: function (result) {
            if (result.result && result.result.timestamp > state.resultTimestamp) {

                state.resultTimestamp = result.result.timestamp;

                var renderResults = renderResult(result.result);
                $(model.div.resultTab).html(renderResults.infoHtml + renderResults.tableHtml);
                $('#resultTable').DataTable({
                    "pageLength": 10
                });
            }
        }
    });

};

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
    }, 10 * 1000);
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

    var renderResults = renderResult(results);
    $(model.div.resultTab).html(renderResults.infoHtml + renderResults.tableHtml);

    $('#resultTable').DataTable({
        "pageLength": 10
    });
};

var renderResult = function (results) {

    console.log("Result: ", results);

    var totalIssues = 0;

    var tableHtml = "";
    tableHtml += "<table id='resultTable' class='table'>\n";
    tableHtml += " <thead>";
    tableHtml += "  <th>repo</th>";
    tableHtml += "  <th>branch</th>";
    tableHtml += "  <th>type</th>";
    tableHtml += "  <th>id</th>";
    tableHtml += "  <th>path</th>";
    tableHtml += "  <th>message</th>";
    tableHtml += "  <th>repairStatus</th>";
    tableHtml += "  <th>repairMessage</th>";
    tableHtml += "  <th>Operations</th>";
    tableHtml += " </thead>";
    tableHtml += "  <tbody>";


    results.repositories.forEach(function (repo) {
        repo.branches.forEach(function (branch) {
            tableHtml += renderBranchResults(repo, branch);
            totalIssues += branch.results.length;
        });
    });

    var date = new Date(results.timestamp);

    var infoHtml = "";
    infoHtml += "<h2 class='resultSummary'>Total issues found: " + totalIssues + "</h2>";
    infoHtml += "<p>Finished: " + date.toISOString() + "</p>";

    tableHtml += "</tbody>";
    tableHtml += "</table>";

    return {infoHtml: infoHtml, tableHtml: tableHtml};
};


var renderBranchResults = function (repo, branch) {

    var rowHtml = "";

    branch.results.forEach(function (entry) {
        rowHtml += "<tr>";
        rowHtml += "<td>" + repo.id + "</td>";
        rowHtml += "<td>" + branch.branch + "</td>";
        rowHtml += "<td>" + entry.type + "</td>";
        rowHtml += "<td>" + entry.id + "</td>";
        rowHtml += "<td>" + entry.path + "</td>";
        rowHtml += "<td>" + entry.message + "</td>";
        rowHtml += "<td>" + entry.repair.status + "</td>";
        rowHtml += "<td>" + entry.repair.message + "</td>";

        if (entry.repair.status === "IS_REPAIRABLE") {
            rowHtml += "<td class='repairOption'><i class='material-icons'>build</i></td>";
        } else {
            rowHtml += "<td></td>";
        }

        rowHtml += "</tr>";
    });

    return rowHtml;
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

            var statusDiv = $(model.div.status);

            if (result.state === "RUNNING") {
                state.taskId = result.taskId;
                $(model.buttons.runValidator).prop("disabled", true);
                $(model.buttons.runValidator + " > i").html("update");
                $(model.buttons.runValidator + " > span").html("Running");
                statusDiv.show();
                toggleState(statusDiv, "ok");
            } else {
                if (result.state === "FAILED") {
                    toggleState(statusDiv, "error");
                    statusDiv.show();
                } else if (result.state === "FINISHED") {
                    statusDiv.show();
                } else {
                    statusDiv.hide();
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