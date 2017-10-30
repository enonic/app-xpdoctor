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
    issues: null
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
        success: function (result) {
            if (result.result && result.result.timestamp > state.resultTimestamp) {
                state.resultTimestamp = result.result.timestamp;
                displayResults(result);
            }
        }
    });

};
var handleJobFinished = function (message) {
    var results = JSON.parse(message.data);
    displayResults(results);
};

var handleRepairAll = function () {

    console.log("Repairing all");

    $(model.buttons.repairLink).each(function (element) {
        console.log("Repair element: ", element);
        repair(element);
        return false;
    });

};

var repair = function (element) {
    var data = {
        nodeId: element.attr('data-nodeid'),
        validatorName: element.attr('data-type'),
        branch: element.attr('data-branch'),
        repoId: element.attr('data-repoId')
    };

    jQuery.ajax({
        url: repairServiceUrl,
        data: data,
        cache: false,
        type: 'GET',
        success: function (result) {
            console.log("RepairResult", result);
            renderRepairResult(result, element);
        }
    });
};

var renderRepairResult = function (result, element) {

    console.log("RepairResult", result);
    var row = element.closest("tr");
    row.addClass("repaired");
    row.children('.repairStatus').html(result.repairStatus.status);
    row.children('.repairMessage').html(result.repairStatus.message);
    row.children('.repairOption').html(' ');
};

var displayResults = function (result) {

    if (!result.result) {
        return;
    }

    state.issues = result.issues;
    var issueRows = renderIssueRows(result.result);
    $(model.table.issuesTableData).html(issueRows);

    var resultTab = $(model.div.resultTab);

    state.dataTable = $(model.table.issuesTable).DataTable({
        searching: false,
        paging: false
    });

    resultTab.show();

    $(model.buttons.repairLink).click(function () {
        var elem = $(this);
        repair(elem);
        return false;
    });
};

var renderIssueRows = function (results) {

    var tableRows = "";

    if (results.totalIssues > 0) {
        $(model.buttons.repairAll).prop("disabled", false);
        results.issues.forEach(function (issue) {
            tableRows += renderIssueRow(issue);
        });
    }
    return tableRows;
};

var renderIssueRow = function (issue) {
    var rowHtml = "<tr>";
    rowHtml += "<td>" + issue.repo + "</td>";
    rowHtml += "<td>" + issue.branch + "</td>";
    rowHtml += "<td>" + issue.type + "</td>";
    rowHtml += "<td>" + issue.id + "</td>";
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
    repairLink += "<a class='repairLink' href='#'" +
                  addData('branch', issue.branch) +
                  addData('repoId', issue.repo) +
                  addData('nodeId', issue.id) +
                  addData('type', issue.validatorName) +
                  ">";
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


