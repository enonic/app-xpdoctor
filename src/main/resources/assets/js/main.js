var model = {
    selectors: {
        repoSelector: "#repoSelector"
    },
    buttons: {
        runValidator: "#btnRunValidator",
        repairAll: "#btnRepairAll",
        repairLink: '.repairLink',
        cancel: "#btnCancel",
        closeOverlay: "#btnCloseOverlay"
    },
    div: {
        results: "#results",
        resultTab: "#resultTab",
        resultMessage: "#resultMessage",
        mainTab: "#mainTab",
        executor: "#executor",
        overlay: "#analysisOverlay",
        overlayProgress: "#overlayProgress",
        overlaySummary: "#overlaySummary",
        overlayRepo: "#overlayRepo",
        overlayValidator: "#overlayValidator",
        progressBarFill: "#progressBarFill"
    },
    item: {
        validatorItemClass: ".validatorItem"
    },
    table: {
        issuesTable: "#resultTable",
        issuesTableData: "#issuesTableData",
        summaryTableBody: "#summaryTableBody"
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
    overlay: null,
    cancelling: false,
    lastResultChecked: false
};

$(function () {

    getTaskState();
    getProgress();
    getLastResult();
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

    $(model.buttons.cancel).click(function () {
        handleCancel();
    });

    $(model.buttons.closeOverlay).click(function () {
        hideOverlay();
    });

    $(model.selectors.repoSelector).change(function () {
        updateButton($(model.buttons.runValidator), false, "play_arrow", "Analyse");
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
        url: repoListApiUrl,
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
    showProgressOverlay();
    doValidation();
    getProgress();
    getTaskState();
};

var handleCancel = function () {
    state.cancelling = true;
    $(model.buttons.cancel).prop("disabled", true).html("Cancelling...");
    $(model.div.overlayValidator).html("Waiting for the current validator to finish...");

    jQuery.ajax({
        url: cancelApiUrl,
        cache: false,
        type: 'POST'
    });
};

var getTaskState = function () {

    jQuery.ajax({
        url: stateApiUrl,
        cache: false,
        type: 'GET',
        success: function (result) {
            handleTaskState(result);
        }
    });
};

var handleTaskState = function (result) {

    var analyzeButton = $(model.buttons.runValidator);
    var repairAllButton = $(model.buttons.repairAll);

    if (result.state === "RUNNING") {
        state.taskId = result.taskId;
        updateButton(analyzeButton, true, "update", "Analysing..");
        updateButton(repairAllButton, true);
        if (!state.overlay) {
            showProgressOverlay();
        }
    } else {
        if (state.cancelling) {
            state.cancelling = false;
            if (state.overlay === 'progress') {
                hideOverlay();
            }
        }

        if (state.repoSelected) {
            updateButton(analyzeButton, false, "play_arrow", "Analyse");
        }
    }
};


var getLastResult = function () {

    jQuery.ajax({
        url: lastResultApiUrl,
        cache: false,
        type: 'GET',
        success: function (serviceResult) {
            if (serviceResult.result && serviceResult.result.timestamp > state.resultTimestamp) {
                state.resultTimestamp = serviceResult.result.timestamp;
                if (state.lastResultChecked) {
                    state.analyzeResult = serviceResult.result;
                    if (state.overlay) {
                        showSummaryOverlay(serviceResult.result);
                    } else {
                        updateIssueTable();
                    }
                }
            }
            state.lastResultChecked = true;
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
        url: repairApiUrl,
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

    if (!state.analyzeResult || !state.analyzeResult.totalIssues) {

        if (state.dataTable) {
            state.dataTable.clear();
            state.dataTable.draw();
        }

        $(model.div.resultTab).addClass("hidden");
        $(model.div.resultMessage).removeClass("hidden");

        return;
    }

    $(model.div.resultMessage).addClass("hidden");
    $(model.div.resultTab).removeClass("hidden");

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

    var data = {
        enabledValidators: getEnabledValidators(),
        repoBranches: repoSelector,
    };

    jQuery.ajax({
        url: validatorApiUrl,
        cache: false,
        type: 'POST',
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
        url: progressApiUrl,
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
    } else if (result.state === "FAILED" && state.overlay === 'progress') {
        hideOverlay();
        $('#repoMessage').html("Analysis failed, see server log for details");
    }
};

var displayProgressInfo = function (result) {

    if (state.cancelling) {
        return;
    }

    var info = result.progress.info;

    $(model.div.overlayRepo).text("Analysing " + info.repositoryId + " - " + info.branch);
    $(model.div.overlayValidator).text("Executing " + info.validator + " (" + info.current + " out of " + info.total + ")...");

    var percent = result.progress.total > 0 ? Math.round(100 * result.progress.current / result.progress.total) : 0;
    $(model.div.progressBarFill).css("width", percent + "%");
};

var showProgressOverlay = function () {
    state.overlay = 'progress';
    state.cancelling = false;
    $(model.buttons.cancel).prop("disabled", false).html("Cancel");
    $(model.div.overlayRepo).text("Analysing...");
    $(model.div.overlayValidator).text("Starting...");
    $(model.div.progressBarFill).css("width", "0%");
    $(model.div.overlaySummary).addClass("hidden");
    $(model.div.overlayProgress).removeClass("hidden");
    $(model.div.overlay).removeClass("hidden");
};

var showSummaryOverlay = function (result) {
    state.overlay = 'summary';
    state.cancelling = false;

    var html = "";
    (result.summary || []).forEach(function (entry) {
        html += "<tr>";
        html += "<td>" + entry.repo + " - " + entry.branch + "</td>";
        html += "<td class='number'>" + entry.analysed + "</td>";
        html += "<td class='number'>" + entry.invalid + "</td>";
        html += "<td class='number'>" + (entry.invalid > 0 ? entry.repairable : "-") + "</td>";
        html += "</tr>";
    });
    $(model.table.summaryTableBody).html(html);

    $(model.buttons.closeOverlay).html(result.totalIssues > 0 ? "Show details" : "Close");

    $(model.div.overlayProgress).addClass("hidden");
    $(model.div.overlaySummary).removeClass("hidden");
    $(model.div.overlay).removeClass("hidden");
};

var hideOverlay = function () {
    state.overlay = null;
    $(model.div.overlay).addClass("hidden");
    updateIssueTable();
};
