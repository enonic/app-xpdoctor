var model = {
    selectors: {},
    buttons: {
        runValidator: "#btnRunValidator",
        repairAll: "#btnRepairAll",
        repairLink: '.repairLink'
    },
    div: {
        status: "#progressReporter",
        results: "#results",
        resultTab: "#resultTab",
        mainTab: "#mainTab",
        executor: "#executor"
    },
    item: {
        progress: "#progressState",
        validatorItem: "#validator_",
        validatorItemClass: ".validatorItem",
        validatorProgressClass: ".itemProgress"
    },
    table: {
        issuesTable: "#resultTable",
        issuesTableData: "#issuesTableData"
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
    resultTimestamp: 0,
    dataTable: null,
    analyzeResult: null
};

$(function () {

    wsConnect();
    getTaskState();
    getProgress();
    initializeButtons();

    setInterval(getProgress, 1000);
    setInterval(getTaskState, 1000);
    setInterval(getLastResult, 2000);

});

var initializeButtons = function () {

    disableButton($(model.buttons.runValidator));
    disableButton($(model.buttons.repairAll));

    $(model.buttons.runValidator).click(function () {
        handleRunValidator();
    });

    $(model.buttons.repairAll).click(function () {
        handleRepairAll();
    });

};

var disableButton = function (button) {
    button.prop("disabled", true);
};


var updateButton = function (button, disabled, icon, label) {
    button.prop("disabled", disabled);

    if (icon) {
        button.children("i").html(icon);
    }

    if (label) {
        button.children("p").html(label);
    }
};

var handleRunValidator = function () {
    clearIssueTable();
    disableButton($(model.buttons.runValidator));
    disableButton($(model.buttons.repairAll));
    doValidation();
    getProgress();
    getTaskState();
};

var getTaskState = function () {

    jQuery.ajax({
        url: stateServiceUrl,
        cache: false,
        type: 'GET',
        success: function (result) {
            handleTaskState(result);
        }
    });
};

var handleTaskState = function (result) {

    var statusDiv = $(model.div.status);
    var analyzeButton = $(model.buttons.runValidator);
    var repairAllButton = $(model.buttons.repairAll);

    if (result.state === "RUNNING") {
        state.taskId = result.taskId;
        updateButton(analyzeButton, true, "update", "Analyzing..");
        updateButton(repairAllButton, true);
        statusDiv.show();
    } else {
        if (result.state === "FAILED") {
            statusDiv.show();
        } else if (result.state === "FINISHED") {
            statusDiv.show();
        } else {
            statusDiv.hide();
        }
        updateButton(analyzeButton, false, "play_arrow", "Analyze");
    }
};


var getLastResult = function () {

    jQuery.ajax({
        url: lastResultServiceUrl,
        cache: false,
        type: 'GET',
        success: function (serviceResult) {
            if (serviceResult.result && serviceResult.result.timestamp > state.resultTimestamp) {
                state.resultTimestamp = serviceResult.result.timestamp;
                state.analyzeResult = serviceResult.result;
                updateIssueTable();
            }
        }
    });

};
var handleJobFinished = function (message) {
    var results = JSON.parse(message.data);
    //updateIssueTable(results);
};

var handleRepairAll = function () {

    if (!state.analyzeResult || !state.analyzeResult.issues) {
        console.log("ERROR: NO RESULTS IN STATE");
    }

    for (var issueKey in state.analyzeResult.issues) {
        repair(issueKey);
    }
};

var repair = function (issueKey) {

    var issue = state.analyzeResult.issues[issueKey];

    var data = {
        nodeId: issue.nodeId,
        validatorName: issue.validatorName,
        branch: issue.branch,
        repoId: issue.repo
    };

    jQuery.ajax({
        url: repairServiceUrl,
        data: data,
        cache: false,
        type: 'GET',
        success: function (result) {
            renderRepairResult(result, issueKey);
        }
    });
};

var renderRepairResult = function (result, issueKey) {

    var item = state.analyzeResult.issues[issueKey];
    item.repairStatus = result.repairStatus.status;
    item.repairMessage = result.repairStatus.message;

    updateIssueTable();
    /*
    var row = $('#' + issueKey);
    row.removeClass();
    row.addClass(result.repairStatus.status);
    row.children('.repairStatus').html(result.repairStatus.status);
    row.children('.repairMessage').html(result.repairStatus.message);
    row.children('.repairOption').html(' ');
    */
};

var updateIssueTable = function () {

    var issuesArray = createAsIssuesArray();

    var dataTable = state.dataTable;

    if (!dataTable) {
        state.dataTable = $(model.table.issuesTable).DataTable({
            data: issuesArray,
            "columns": [
                {"data": "repo"},
                {"data": "branch"},
                {"data": "type"},
                {"data": "nodeId"},
                {"data": "path"},
                {"data": "message"},
                {"data": "validatorName"},
                {"data": "repairStatus"},
                {"data": "repairMessage"}
            ],
            searching: false,
            paging: false,
            "rowCallback": function (row, data, index) {
                styleRow(row, data, index);
            }
        });
    } else {
        dataTable.clear();
        dataTable.rows.add(issuesArray);
        dataTable.draw();
    }

    if (state.analyzeResult.totalIssues > 0) {
        updateButton($(model.buttons.repairAll), false);
    }

};

var styleRow = function (row, data, index) {

    if (data.repairStatus) {
        $(row).removeClass();
        $(row).addClass(data.repairStatus);
    }
};

var createAsIssuesArray = function () {
    var issuesArray = [];

    for (var key in state.analyzeResult.issues) {
        issuesArray.push(state.analyzeResult.issues[key]);
    }

    console.log("issuesArray:", issuesArray);
    return issuesArray;
};

var clearIssueTable = function () {

    state.analyzeResult = null;
    $(model.table.issuesTableData).html("");
};

var renderIssueRows = function (analyzeResult) {

    console.log("ANALYZE-RESULT: ", analyzeResult.issues);

    var tableRows = "";

    if (analyzeResult.totalIssues > 0) {
        $(model.buttons.repairAll).prop("disabled", false);

        for (var issueKey in analyzeResult.issues) {
            tableRows += renderIssueRow(issueKey, analyzeResult.issues[issueKey]);
        }
    }
    return tableRows;
};

var renderIssueRow = function (key, issue) {
    var rowHtml = "<tr id='" + key + "' class='" + issue.repairStatus + "'>";
    rowHtml += "<td>" + issue.repo + "</td>";
    rowHtml += "<td>" + issue.branch + "</td>";
    rowHtml += "<td>" + issue.type + "</td>";
    rowHtml += "<td>" + issue.nodeId + "</td>";
    rowHtml += "<td>" + issue.path + "</td>";
    rowHtml += "<td>" + issue.message + "</td>";
    rowHtml += "<td class='repairStatus'>" + issue.repairStatus + "</td>";
    rowHtml += "<td class='repairMessage'>" + issue.repairMessage + "</td>";
    rowHtml += "<td class='repairOption'>" + renderRepairOptions(issue) + "</td>";
    rowHtml += "</tr>";
    return rowHtml;
};

var renderRepairOptions = function (issue) {

    var repairOptions = "";

    if (issue.repairStatus === "IS_REPAIRABLE") {
        repairOptions += renderRepairLink(issue);
    }

    return repairOptions;
};

var renderRepairLink = function (issue) {
    var repairLink = "";
    repairLink += "<a class='repairLink' href='#'>";
    repairLink += "<i class='material-icons'>build</i>";
    repairLink += "</a>";
    return repairLink;
};

var addData = function (name, value) {
    return " data-" + name + "=" + value;
};

var doValidation = function () {
    var data = {
        enabledValidators: getEnabledValidators()
    };

    jQuery.ajax({
        url: validatorServiceUrl,
        cache: false,
        type: 'GET',
        data: data,
        success: function (result) {
            if (result.error) {
                $('#repoMessage').html("result.error");
            }
        }
    });
};

var getEnabledValidators = function () {
    var validatorArray = [];

    $(model.item.validatorItemClass + ' input:checked').each(function () {
            validatorArray.push($(this).attr('data-validatorName'));
        }
    );
    return validatorArray;
};

var getProgress = function () {

    var data = {
        taskId: state.taskId
    };

    jQuery.ajax({
        url: progressServiceUrl,
        data: data,
        cache: false,
        type: 'GET',
        success: function (result) {
            renderProgress(result);
        }
    });
};

var renderProgress = function (result) {
    if (result.state === "RUNNING") {
        displayProgressInfo(result);
        markCurrentValidator(result);
    } else {
        resetProgress();
    }
};

var resetProgress = function () {

    $(model.item.progress).html("");
    $(model.item.validatorItemClass).each(function () {
        $(this).removeClass("active");
        $(this).find(model.item.validatorProgressClass).html("");
    });
};

var displayProgressInfo = function (result) {

    var progressMessage = result.state;
    progressMessage += " [ ";
    progressMessage += result.progress.info.repositoryId;
    progressMessage += "/";
    progressMessage += result.progress.info.branch;
    progressMessage += " ] ";
    $(model.item.progress).html(progressMessage);
};

var markCurrentValidator = function (result) {

    var infoElement = result.progress.info;
    var currentRunning = getRunningValidator(infoElement);

    if (!currentRunning) {
        console.log("Current running not found", infoElement.validator);
        return;
    }

    $(model.item.validatorItemClass).each(function () {
        var validatorItem = $(this);

        if (currentRunning.is(validatorItem)) {
            markAsRunning(validatorItem, result.progress);
        } else {
            removeActiveMark(validatorItem);
        }
    });
};

var getRunningValidator = function (infoElement) {
    return $(model.item.validatorItem + infoElement.validator);
};

var markAsRunning = function (element, info) {
    element.addClass("active");
    var progressInfoMessage = " (" + info.current + "/" + info.total + ") ";
    getProgressInfoElement(element).html(progressInfoMessage);
};

var removeActiveMark = function (element) {
    element.removeClass("active");
    getProgressInfoElement(element).html("");
};

var getProgressInfoElement = function (element) {
    return element.find(model.item.validatorProgressClass);
};


// ----------- WS ------------
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

    if (message.type === "jobFinished") {
        handleJobFinished(message);
    }
}


