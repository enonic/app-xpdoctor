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
    repoSelected: false
};

$(function () {

    getTaskState();
    getProgress();
    initializeButtons();

    setInterval(getProgress, 1000);
    setInterval(getTaskState, 1000);
    setInterval(getLastResult, 2000);

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

    html += "<option selected='true' disabled='disabled'>Select repository/branch</option>";

    result.repoList.forEach(function (entry) {
        html += "<option value='" + entry.repo + ";" + entry.branch + "'>";
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
            if (serviceResult.result && serviceResult.result.timestamp > state.resultTimestamp) {
                state.resultTimestamp = serviceResult.result.timestamp;
                state.analyzeResult = serviceResult.result;
                updateIssueTable();
            }
        }
    });

};

var handleRepairAll = function () {

    if (!state.analyzeResult || !state.analyzeResult.issues) {
        console.log("ERROR: NO RESULTS IN STATE");
    }

    for (var issueKey in state.analyzeResult.issues) {
        if (state.analyzeResult.issues[issueKey].repairStatus === "IS_REPAIRABLE") {
            repair(issueKey);
        }
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

    if (state.analyzeResult.totalIssues > 0) {
        updateButton($(model.buttons.repairAll), false);
    }

    $(model.buttons.repairLink).click(function () {
        var button = $(this);
        var issueId = button.attr("data-issueId");
        repair(issueId);
        return false;
    });

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
                "createdCell": function (td, cellData, rowData, row, col) {
                    $(td).addClass("repairOption");
                    $(td).html(renderRepairOptions(rowData));
                }
            }
        ],
        searching: false,
        paging: false,
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

    var values = repoSelector.split(";");

    var data = {
        enabledValidators: getEnabledValidators(),
        repoId: values[0],
        branch: values[1]
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



