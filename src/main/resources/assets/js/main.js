var model = {
    selectors: {
        repoSelector: "#repoSelector"
    },
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
    analyzeResult: null,
    repoSelected: false,
    repairVersion: 0,
    repairing: false,
    lastResultChecked: false,
    running: false,
    pollTimer: null,
    pollTick: 0,
    expectTaskUntil: null
};

var ACTIVE_POLL_MS = 1000;
var TASK_DISCOVERY_MS = 5000;

$(function () {

    getLastResult();
    getTaskState();
    initializeButtons();

    loadRepoList();
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

    $(model.table.issuesTable).on("click", model.buttons.repairLink, function () {
        startRepair({issueIds: [$(this).attr("data-issueId")]});
        return false;
    });

    $(model.selectors.repoSelector).change(function () {
        updateButton($(model.buttons.runValidator), false, "play_arrow", "Analyze");
        state.repoSelected = true;
    })

    const checkUncheckAll = $('#toggleAll');
    const checkboxes = $(model.item.validatorItemClass + ' input');

    checkUncheckAll.on('click', function() {
        const firstCheckbox = checkboxes.first();
        const isChecked = !firstCheckbox.prop('checked');
        checkboxes.prop('checked', isChecked);
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


var loadRepoList = function () {
    jQuery.ajax({
        url: repoListServiceUrl,
        cache: false,
        type: 'GET',
        success: function (result) {
            populateRepoSelector(result);
        }
    });
};

var populateRepoSelector = function (result) {

    console.log("LoadRepoList result:", result);

    var html = "";

    result.repoList.forEach(function (entry) {
        html += "<option value='" + entry.repo + ":" + entry.branch + "'>";
        html += entry.repo + " - " + entry.branch + " (" + entry.count + " nodes)";
        html += "</option>";
    });

    $(model.selectors.repoSelector).html(html);

};

var handleRunValidator = function () {
    clearIssueTable();
    disableButton($(model.buttons.runValidator));
    disableButton($(model.buttons.repairAll));
    doValidation();
};

var getTaskState = function () {

    jQuery.ajax({
        url: stateServiceUrl,
        cache: false,
        type: 'GET',
        success: function (result) {
            handleTaskState(result);
        },
        complete: schedulePoll
    });
};

var schedulePoll = function () {
    clearTimeout(state.pollTimer);
    if (state.running) {
        state.pollTimer = setTimeout(getTaskState, ACTIVE_POLL_MS);
    }
};

var handleTaskState = function (result) {

    var statusDiv = $(model.div.status);
    var analyzeButton = $(model.buttons.runValidator);
    var repairAllButton = $(model.buttons.repairAll);

    var active = result.state === "RUNNING" || result.state === "WAITING";

    if (!active && state.expectTaskUntil && Date.now() < state.expectTaskUntil) {
        return;
    }
    state.expectTaskUntil = null;

    var wasRunning = state.running;
    state.running = active;

    if (active) {
        state.taskId = result.taskId;
        getProgress();
        if (result.type === "repair" && state.pollTick++ % 2 === 0) {
            getLastResult();
        }
        if (result.type === "repair") {
            state.repairing = true;
            disableButton(analyzeButton);
        } else {
            updateButton(analyzeButton, true, "update", "Analyzing..");
        }
        updateButton(repairAllButton, true);
        statusDiv.show();
    } else {
        if (wasRunning) {
            getProgress();
            getLastResult();
        }

        if (state.repairing) {
            state.repairing = false;
            updateRepairAllButton();
        }

        if (result.state === "FAILED") {
            statusDiv.show();
        } else if (result.state === "FINISHED") {
            statusDiv.show();
        } else {
            statusDiv.hide();
        }

        if (state.repoSelected) {
            updateButton(analyzeButton, false, "play_arrow", "Analyze");
        }
    }
};


var getLastResult = function () {

    jQuery.ajax({
        url: lastResultServiceUrl,
        cache: false,
        type: 'GET',
        success: function (serviceResult) {
            var result = serviceResult.result;
            if (!result || !result.timestamp) {
                state.lastResultChecked = true;
                return;
            }

            if (result.timestamp > state.resultTimestamp) {
                state.resultTimestamp = result.timestamp;
                state.repairVersion = result.repairVersion || 0;
                if (state.lastResultChecked) {
                    state.analyzeResult = result;
                    updateIssueTable();
                }
            } else if (result.timestamp === state.resultTimestamp && result.repairVersion > state.repairVersion) {
                state.repairVersion = result.repairVersion;
                if (state.analyzeResult) {
                    applyRepairUpdates(result);
                }
            }
            state.lastResultChecked = true;
        }
    });

};

var handleRepairAll = function () {
    startRepair({all: true});
};

var startRepair = function (params) {

    if (!state.analyzeResult || !state.analyzeResult.issues) {
        console.log("ERROR: NO RESULTS IN STATE");
        return;
    }

    disableButton($(model.buttons.repairAll));

    jQuery.ajax({
        url: repairServiceUrl,
        data: params,
        cache: false,
        type: 'POST',
        success: function (result) {
            if (result.error) {
                $('#repoMessage').html(result.error);
                updateRepairAllButton();
                return;
            }
            state.repairing = true;
            expectTask(result.taskId);
        },
        error: function () {
            $('#repoMessage').html("Repair request failed");
            updateRepairAllButton();
        }
    });
};

var expectTask = function (taskId) {
    state.running = true;
    state.taskId = taskId;
    state.expectTaskUntil = Date.now() + TASK_DISCOVERY_MS;
    getTaskState();
};

var applyRepairUpdates = function (result) {

    var changed = false;

    for (var issueKey in result.issues) {
        var fresh = result.issues[issueKey];
        var item = state.analyzeResult.issues[issueKey];

        if (item && (item.repairStatus !== fresh.repairStatus || item.repairMessage !== fresh.repairMessage)) {
            item.repairStatus = fresh.repairStatus;
            item.repairMessage = fresh.repairMessage;
            changed = true;

            if (state.dataTable) {
                var row = state.dataTable.row("#" + issueKey);
                if (row.any()) {
                    row.invalidate();
                }
            }
        }
    }

    if (changed && state.dataTable) {
        state.dataTable.draw(false);
    }

    updateRepairAllButton();
};

var countRepairableIssues = function () {
    var count = 0;
    if (state.analyzeResult && state.analyzeResult.issues) {
        for (var key in state.analyzeResult.issues) {
            if (state.analyzeResult.issues[key].repairStatus === "IS_REPAIRABLE") {
                count++;
            }
        }
    }
    return count;
};

var updateRepairAllButton = function () {
    $(model.buttons.repairAll).prop("disabled", state.repairing || countRepairableIssues() === 0);
};

var updateIssueTable = function () {

    if (!state.analyzeResult) {

        if (state.dataTable) {
            state.dataTable.clear();
            state.dataTable.draw();
        }

        return;
    }

    var issuesArray = createAsIssuesArray();
    var dataTable = state.dataTable;

    if (!dataTable) {
        initializeDataTable(issuesArray);
    } else {
        dataTable.clear();
        dataTable.rows.add(issuesArray);
        dataTable.draw();
    }

    updateRepairAllButton();

};

var initializeDataTable = function (issuesArray) {
    state.dataTable = $(model.table.issuesTable).DataTable({
        data: issuesArray,
        "columns": [
            {
                "data": "repo",
                "title": "repo"
            },
            {
                "data": "branch",
                "title": "branch"
            },
            {
                "data": "type",
                "title": "type"
            },
            {
                "data": "nodeId",
                "title": "nodeId"
            },
            {
                "data": "nodeVersionId",
                "defaultContent": '',
                "title": "nodeVersionId"
            },
            {
                "data": "path",
                "defaultContent": '',
                "title": "path"
            },
            {
                "data": "message",
                "title": "message"
            },
            {
                "data": "repairStatus",
                "title": "repairStatus"
            },
            {
                "data": "repairMessage",
                "title": "repairMessage"
            },
            {
                "data": null,
                "defaultContent": '',
                "title": "repairOptions",
                "orderable": false,
                "className": "repairOption",
                "render": function (data, type, rowData) {
                    return renderRepairOptions(rowData);
                }
            }
        ],
        rowId: "issueId",
        searching: false,
        paging: true,
        pageLength: 50,
        lengthMenu: [25, 50, 100, 250],
        deferRender: true,
        "rowCallback": function (row, data, index) {
            postProcessRow(row, data, index);
        }
    });
};

var renderRepairOptions = function (issue) {
    var repairOptions = "";
    if (issue.repairStatus === "IS_REPAIRABLE") {
        repairOptions += renderRepairLink(issue);
    }
    if (issue.repairStatus === "FAILED") {
        // repairOptions += renderDeleteLink(issue);
    }
    return repairOptions;
};

var renderRepairLink = function (issue) {

    var repairLink = "";
    repairLink += "<a class='repairLink' href='#' data-issueId='" + issue.issueId + "'>";
    repairLink += "<i class='material-icons'>build</i>";
    repairLink += "</a>";
    return repairLink;
};

var renderDeleteLink = function (issue) {
    var repairLink = "";
    repairLink += "<a class='deleteLink' href='#'>";
    repairLink += "<i class='material-icons'>delete</i>";
    repairLink += "</a>";
    return repairLink;
};

var postProcessRow = function (row, data, index) {

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

    return issuesArray;
};

var clearIssueTable = function () {

    state.analyzeResult = null;
    updateIssueTable();
};

var doValidation = function () {

    var repoSelector = $(model.selectors.repoSelector).val();

    console.log("REPOSELECTOR: ", repoSelector);

    var data = {
        enabledValidators: getEnabledValidators(),
        repoBranches: repoSelector,
    };

    jQuery.ajax({
        url: validatorServiceUrl,
        cache: false,
        type: 'POST',
        data: data,
        success: function (result) {
            if (result.error) {
                $('#repoMessage').html(result.error);
                return;
            }
            expectTask(result.taskId);
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
        if (!isRepairTask(result)) {
            markCurrentValidator(result);
        }
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

var isRepairTask = function (result) {
    return !!result.description && /\.repair$/.test(result.description);
};

var displayProgressInfo = function (result) {

    if (isRepairTask(result)) {
        $(model.item.progress).html("Repairing " + result.progress.current + " of " + result.progress.total + "...");
        return;
    }

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
    var currentRunning = getRunningValidator(infoElement.validator);

    if (!currentRunning) {
        console.log("Current running not found", infoElement.validator);
        return;
    }

    $(model.item.validatorItemClass).each(function () {
        var validatorItem = $(this);

        if (currentRunning.is(validatorItem)) {
            markAsRunning(validatorItem, result.progress.current, result.progress.total);
        } else {
            removeActiveMark(validatorItem);
        }
    });
};

var getRunningValidator = function (validator) {
    return $(model.item.validatorItem + validator);
};

var markAsRunning = function (element, current, total) {
    element.addClass("active");
    var progressInfoMessage = " (" + current + "/" + total + ") ";
    getProgressInfoElement(element).html(progressInfoMessage);
};

var removeActiveMark = function (element) {
    element.removeClass("active");
    getProgressInfoElement(element).html("");
};

var getProgressInfoElement = function (element) {
    return element.find(model.item.validatorProgressClass);
};



