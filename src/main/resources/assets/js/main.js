var model = {
    buttons: {
        newAnalysis: "#btnNewAnalysis",
        analyse: "#btnAnalyse",
        closeSetup: "#btnCloseSetup",
        repairAll: "#btnRepairAll",
        repairLink: '.repairLink',
        cancel: "#btnCancel",
        repairSummary: "#btnRepairSummary",
        closeOverlay: "#btnCloseOverlay"
    },
    div: {
        resultWrapper: "#resultWrapper",
        repairProgress: "#repairProgress",
        pageLength: "#pageLength",
        overlay: "#analysisOverlay",
        overlaySetup: "#overlaySetup",
        overlayProgress: "#overlayProgress",
        overlaySummary: "#overlaySummary",
        overlayRepo: "#overlayRepo",
        overlayValidator: "#overlayValidator",
        progressBar: "#progressBar",
        progressBarFill: "#progressBarFill",
        repoTree: "#repoTree",
        validatorList: "#validatorList",
        selectionSummary: "#selectionSummary"
    },
    input: {
        selectAllValidators: "#selectAllValidators",
        validatorCheckbox: ".validatorCheckbox",
        treeCheckbox: ".treeCheckbox",
        branchCheckbox: ".branchCheckbox"
    },
    table: {
        issuesTable: "#resultTable",
        summaryTableBody: "#summaryTableBody"
    }
};

var REPAIRED_ROW_DELAY_MS = 2500;
var REPAIR_REQUEST_TIMEOUT_MS = 120000;

var repoGroups = [
    {key: "cms", title: "CMS", expanded: true, matches: function (repo) { return repo.indexOf("com.enonic.cms.") === 0; }},
    {key: "system", title: "System", expanded: false, matches: function (repo) { return repo.indexOf("system.") === 0 || repo === "system-repo"; }},
    {key: "custom", title: "Custom", expanded: false, matches: function () { return true; }}
];

var state = {
    taskId: null,
    resultTimestamp: 0,
    dataTable: null,
    analyzeResult: null,
    overlay: null,
    cancelling: false,
    lastResultChecked: false,
    running: false,
    pollTimer: null,
    repairQueue: [],
    repairTotal: 0,
    repairing: false,
    repairActive: null,
    repairTimedOut: false
};

var ACTIVE_POLL_MS = 1000;

$(function () {

    getLastResult();
    getTaskState();
    initializeButtons();
    showSetupOverlay();

    loadRepoList();
});

var initializeButtons = function () {

    $(model.buttons.newAnalysis).click(function () {
        showSetupOverlay();
    });

    $(model.buttons.analyse).click(function () {
        handleRunValidator();
    });

    $(model.buttons.closeSetup).click(function () {
        hideOverlay();
    });

    $(model.buttons.repairAll).click(function () {
        handleRepairAll();
    });

    $(model.table.issuesTable).on("click", model.buttons.repairLink, function () {
        enqueueRepair([$(this).attr("data-issueId")]);
        return false;
    });

    $(model.div.pageLength).change(function () {
        if (state.dataTable) {
            state.dataTable.page.len(parseInt($(this).val(), 10)).draw();
        }
    });

    $(model.buttons.cancel).click(function () {
        handleCancel();
    });

    $(model.buttons.closeOverlay).click(function () {
        hideOverlay();
    });

    $(model.buttons.repairSummary).click(function () {
        hideOverlay();
        handleRepairAll();
    });

    $(model.input.selectAllValidators).change(function () {
        $(model.input.validatorCheckbox).prop("checked", this.checked);
        updateSetupState();
    });

    $(model.div.validatorList).on("change", model.input.validatorCheckbox, function () {
        updateSetupState();
    });

    $(model.div.repoTree).on("change", model.input.treeCheckbox, function () {
        handleTreeCheckboxChange($(this));
    });

    $(model.div.repoTree).on("click", ".treeGroupHeader", function () {
        var $group = $(this).closest(".treeGroup");
        $group.toggleClass("collapsed");
        $(this).find("i").text($group.hasClass("collapsed") ? "chevron_right" : "expand_more");
    });

    $(document).keydown(function (e) {
        if (e.key === "Escape" && ((state.overlay === "setup" && hasIssues()) || state.overlay === "summary")) {
            hideOverlay();
        }
    });
};

var loadRepoList = function () {
    jQuery.ajax({
        url: repoListApiUrl,
        cache: false,
        type: 'GET',
        success: function (result) {
            renderRepoTree(result.repoList);
        }
    });
};

var renderRepoTree = function (repoList) {

    var grouped = {};
    repoGroups.forEach(function (group) {
        grouped[group.key] = {};
    });

    repoList.forEach(function (entry) {
        var group = repoGroups.filter(function (g) { return g.matches(entry.repo); })[0];
        var repos = grouped[group.key];
        if (!repos[entry.repo]) {
            repos[entry.repo] = [];
        }
        repos[entry.repo].push(entry);
    });

    var html = "";

    repoGroups.forEach(function (group) {
        var repoIds = Object.keys(grouped[group.key]).sort();
        if (repoIds.length === 0) {
            return;
        }

        html += "<div class='treeGroup" + (group.expanded ? "" : " collapsed") + "'>";
        html += "<div class='treeRow level0 treeGroupHeader'>";
        html += "<i class='material-icons'>" + (group.expanded ? "expand_more" : "chevron_right") + "</i>";
        html += "<span class='treeLabel'>" + group.title + "</span>";
        html += "<span class='treeCount'>" + repoIds.length + " repos</span></div>";
        html += "<div class='treeChildren'>";

        repoIds.forEach(function (repoId) {
            var branches = grouped[group.key][repoId];
            html += "<div class='treeRepo'>";
            html += treeRow(1, "<input type='checkbox' class='treeCheckbox'>", repoId, "");
            html += "<div class='treeChildren'>";
            branches.forEach(function (branch) {
                var checkbox = "<input type='checkbox' class='treeCheckbox branchCheckbox' value='" + repoId + ":" + branch.branch +
                               "' data-count='" + branch.count + "'>";
                html += treeRow(2, checkbox, branch.branch, branch.count + " nodes");
            });
            html += "</div></div>";
        });

        html += "</div></div>";
    });

    $(model.div.repoTree).html(html);
    updateSetupState();
};

var treeRow = function (level, checkbox, label, count) {
    return "<label class='treeRow level" + level + "'>" + checkbox +
           "<span class='treeLabel'>" + label + "</span>" +
           "<span class='treeCount'>" + count + "</span></label>";
};

var handleTreeCheckboxChange = function ($checkbox) {
    var checked = $checkbox.prop("checked");

    $checkbox.closest(".treeRow").next(".treeChildren").find("input").prop({checked: checked, indeterminate: false});

    var $container = $checkbox.closest(".treeChildren");
    while ($container.length) {
        var $parentRow = $container.prev(".treeRow");
        var branches = $container.find(model.input.branchCheckbox);
        var checkedCount = branches.filter(":checked").length;
        $parentRow.find("input").prop({
            checked: checkedCount > 0 && checkedCount === branches.length,
            indeterminate: checkedCount > 0 && checkedCount < branches.length
        });
        $container = $parentRow.closest(".treeChildren");
    }

    updateSetupState();
};

var updateSetupState = function () {
    var branches = $(model.input.branchCheckbox + ":checked");
    var nodes = 0;
    branches.each(function () {
        nodes += parseInt($(this).attr("data-count"), 10) || 0;
    });

    var validators = $(model.input.validatorCheckbox);
    var checkedValidators = validators.filter(":checked").length;

    $(model.input.selectAllValidators).prop({
        checked: checkedValidators === validators.length,
        indeterminate: checkedValidators > 0 && checkedValidators < validators.length
    });

    var summary = "";
    if (branches.length > 0) {
        summary = branches.length + (branches.length === 1 ? " branch, " : " branches, ") + nodes + " nodes, " +
                  checkedValidators + (checkedValidators === 1 ? " validator" : " validators");
    }
    $(model.div.selectionSummary).text(summary);

    $(model.buttons.analyse).prop("disabled", branches.length === 0 || checkedValidators === 0);
};

var handleRunValidator = function () {
    clearIssueTable();
    showProgressOverlay();
    doValidation();
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

    var wasRunning = state.running;
    state.running = result.state === "RUNNING";

    if (result.state === "RUNNING") {
        state.taskId = result.taskId;
        getProgress();
        $(model.buttons.newAnalysis).prop("disabled", true);
        $(model.buttons.repairAll).prop("disabled", true);
        if (state.overlay !== 'progress' && state.overlay !== 'summary') {
            showProgressOverlay();
        }
    } else {
        if (wasRunning) {
            getProgress();
            getLastResult();
        }

        if (state.cancelling) {
            state.cancelling = false;
            if (state.overlay === 'progress') {
                hideOverlay();
            }
        }

        $(model.buttons.newAnalysis).prop("disabled", false);
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
                    if (state.overlay === 'progress' || state.overlay === 'summary') {
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
        return;
    }

    var issueKeys = [];
    for (var issueKey in state.analyzeResult.issues) {
        if (state.analyzeResult.issues[issueKey].repairStatus === "IS_REPAIRABLE") {
            issueKeys.push(issueKey);
        }
    }

    enqueueRepair(issueKeys);
};

var enqueueRepair = function (issueKeys) {
    var result = state.analyzeResult;

    issueKeys.forEach(function (issueKey) {
        if (!isRepairPending(result, issueKey)) {
            state.repairQueue.push({result: result, issueKey: issueKey});
            state.repairTotal++;
        }
    });

    if (!state.repairing) {
        processRepairQueue();
    }
};

var isRepairPending = function (result, issueKey) {
    var active = state.repairActive;
    if (active && active.result === result && active.issueKey === issueKey) {
        return true;
    }
    return state.repairQueue.some(function (entry) {
        return entry.result === result && entry.issueKey === issueKey;
    });
};

var processRepairQueue = function () {

    if (state.repairTimedOut) {
        state.repairTimedOut = false;
        state.repairQueue = [];
        $('#repoMessage').html("A repair request timed out. The server may still be repairing that node; remaining repairs were skipped.");
    }

    if (state.repairQueue.length === 0) {
        state.repairing = false;
        state.repairTotal = 0;
        $(model.div.repairProgress).addClass("hidden");
        $(model.buttons.newAnalysis).prop("disabled", false);
        updateRepairAllButton();
        return;
    }

    state.repairing = true;
    $(model.buttons.repairAll).prop("disabled", true);
    $(model.buttons.newAnalysis).prop("disabled", true);

    var entry = state.repairQueue.shift();
    var done = state.repairTotal - state.repairQueue.length;
    $(model.div.repairProgress).removeClass("hidden").text("Repairing " + done + " of " + state.repairTotal + "...");

    state.repairActive = entry;
    repair(entry, function () {
        state.repairActive = null;
        processRepairQueue();
    });
};

var repair = function (entry, callback) {

    var issue = entry.result === state.analyzeResult ? entry.result.issues[entry.issueKey] : null;

    if (!issue) {
        callback();
        return;
    }

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
        timeout: REPAIR_REQUEST_TIMEOUT_MS,
        success: function (result) {
            renderRepairResult(result, entry);
        },
        error: function (xhr, textStatus) {
            if (textStatus === "timeout") {
                state.repairTimedOut = true;
                renderRepairResult({repairStatus: {status: "FAILED", message: "Repair request timed out"}}, entry);
                return;
            }
            renderRepairResult({repairStatus: {status: "FAILED", message: "Repair request failed"}}, entry);
        },
        complete: callback
    });
};

var renderRepairResult = function (result, entry) {

    var item = entry.result.issues[entry.issueKey];
    if (!item) {
        return;
    }

    item.repairStatus = result.repairStatus.status;
    item.repairMessage = result.repairStatus.message;

    if (entry.result !== state.analyzeResult) {
        return;
    }

    if (state.dataTable) {
        var row = state.dataTable.row("#" + entry.issueKey);
        if (row.any()) {
            row.invalidate().draw(false);
        }
    }

    if (item.repairStatus === "REPAIRED") {
        setTimeout(function () {
            removeRepairedIssue(entry.result, entry.issueKey);
        }, REPAIRED_ROW_DELAY_MS);
    }
};

var removeRepairedIssue = function (result, issueKey) {

    if (state.analyzeResult !== result || !result.issues[issueKey]) {
        return;
    }

    delete result.issues[issueKey];
    result.totalIssues = Object.keys(result.issues).length;

    if (state.dataTable) {
        var row = state.dataTable.row("#" + issueKey);
        if (row.any()) {
            row.remove().draw(false);
        }
    }
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

var hasIssues = function () {
    return !!(state.analyzeResult && state.analyzeResult.totalIssues);
};

var updateIssueTable = function () {

    if (!hasIssues()) {

        if (state.dataTable) {
            state.dataTable.clear();
            state.dataTable.draw();
        }

        $(model.div.resultWrapper).addClass("hidden");

        return;
    }

    $(model.div.resultWrapper).removeClass("hidden");

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
        pageLength: parseInt($(model.div.pageLength).val(), 10),
        deferRender: true,
        layout: {
            topStart: null,
            topEnd: null,
            bottomStart: 'info',
            bottomEnd: 'paging'
        },
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
    return repairOptions;
};

var renderRepairLink = function (issue) {

    var repairLink = "";
    repairLink += "<a class='repairLink' href='#' data-issueId='" + issue.issueId + "'>";
    repairLink += "<i class='material-icons'>build</i>";
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

    var repoBranches = $(model.input.branchCheckbox + ":checked").map(function () {
        return $(this).val();
    }).get();

    var data = {
        enabledValidators: getEnabledValidators(),
        repoBranches: repoBranches,
    };

    jQuery.ajax({
        url: validatorApiUrl,
        cache: false,
        type: 'POST',
        data: data,
        success: function (result) {
            if (result.error) {
                $('#repoMessage').html(result.error);
                return;
            }
            state.running = true;
            state.taskId = result.taskId;
            getTaskState();
        }
    });

};

var getEnabledValidators = function () {
    return $(model.input.validatorCheckbox + ":checked").map(function () {
        return $(this).attr('data-validatorName');
    }).get();
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

    if (info && info.validator) {
        $(model.div.overlayRepo).text("Analysing " + info.repositoryId + " - " + info.branch);
        $(model.div.overlayValidator).text("Executing " + info.validator + " (" + info.current + " out of " + info.total + ")...");
    }

    var percent = result.progress.total > 0 ? Math.round(100 * result.progress.current / result.progress.total) : 0;
    $(model.div.progressBarFill).css("width", percent + "%");
    $(model.div.progressBar).attr("aria-valuenow", percent);
};

var showOverlaySection = function (section) {
    $(model.div.overlaySetup + ", " + model.div.overlayProgress + ", " + model.div.overlaySummary).addClass("hidden");
    $(section).removeClass("hidden");
    $(model.div.overlay).removeClass("hidden");
    $("body").addClass("overlayOpen");
};

var showSetupOverlay = function () {
    state.overlay = 'setup';
    $(model.buttons.closeSetup).toggleClass("hidden", !hasIssues());
    updateSetupState();
    showOverlaySection(model.div.overlaySetup);
};

var showProgressOverlay = function () {
    state.overlay = 'progress';
    state.cancelling = false;
    $(model.buttons.cancel).prop("disabled", false).html("Cancel");
    $(model.div.overlayRepo).text("Analysing...");
    $(model.div.overlayValidator).text("Starting...");
    $(model.div.progressBarFill).css("width", "0%");
    $(model.div.progressBar).attr("aria-valuenow", 0);
    showOverlaySection(model.div.overlayProgress);
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

    $(model.buttons.closeOverlay).html(result.totalIssues > 0 ? "Show issues" : "Close");
    $(model.buttons.repairSummary).toggleClass("hidden", countRepairableIssues() === 0);

    showOverlaySection(model.div.overlaySummary);
};

var hideOverlay = function () {
    state.overlay = null;
    $(model.div.overlay).addClass("hidden");
    $("body").removeClass("overlayOpen");
    updateIssueTable();
    if (!hasIssues()) {
        showSetupOverlay();
    }
};
