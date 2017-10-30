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
        issuesTable: "#resultTable"
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
    dataTable: null
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

var repair = function (element) {

    console.log("Enter the repair function", element);

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

    var renderResults = renderIssuesData(result.result);
    var resultTab = $(model.div.resultTab);

    resultTab.html(renderResults.infoHtml + renderResults.tableHtml);
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

var renderIssuesData = function (results) {
    var infoHtml = "";
    infoHtml += "<h2 class='resultSummary'>Total issues found: " + results.totalIssues + "</h2>";
    var issueTable = renderIssueTable(results);

    return {infoHtml: infoHtml, tableHtml: issueTable};
};

var renderIssueTable = function (results) {
    var errorsHtml = "";

    if (results.totalIssues > 0) {
        $(model.buttons.repairAll).prop("disabled", false);
        errorsHtml += "<table id='resultTable' class='table'>";
        errorsHtml += " <thead>";
        errorsHtml += "  <th>repo</th>";
        errorsHtml += "  <th>branch</th>";
        errorsHtml += "  <th>type</th>";
        errorsHtml += "  <th>id</th>";
        errorsHtml += "  <th>path</th>";
        errorsHtml += "  <th>message</th>";
        errorsHtml += "  <th>repairStatus</th>";
        errorsHtml += "  <th>repairMessage</th>";
        errorsHtml += "  <th>Operations</th>";
        errorsHtml += " </thead>";
        errorsHtml += " <tbody>";

        results.repositories.forEach(function (repo) {
            repo.branches.forEach(function (branch) {
                errorsHtml += renderBranchResults(repo, branch);
            });
        });

        errorsHtml += "</tbody>";
        errorsHtml += "</table>";
    }
    return errorsHtml;
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
        rowHtml += "<td class='repairStatus'>" + entry.repair.status + "</td>";
        rowHtml += "<td class='repairMessage'>" + entry.repair.message + "</td>";
        rowHtml += "<td class='repairOption'>" + renderRepairOptions(entry, branch, repo) + "</td>";
        rowHtml += "</tr>";
    });

    return rowHtml;
};

var renderRepairOptions = function (entry, branch, repo) {

    var repairOptions = "";

    if (entry.repair.status === "IS_REPAIRABLE") {
        repairOptions += renderRepairLink(branch, repo, entry);
    }

    return repairOptions;
};

var renderRepairLink = function (branch, repo, entry) {
    var repairLink = "";
    repairLink += "<a class='repairLink' href='#'" +
                  addData('branch', branch.branch) +
                  addData('repoId', repo.id) +
                  addData('nodeId', entry.id) +
                  addData('type', entry.validatorName) +
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


