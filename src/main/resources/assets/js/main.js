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
    lastResultChecked: false
};

$(function () {

    getTaskState();
    getProgress();
    getLastResult();
    initializeButtons();
    showSetupOverlay();

    setInterval(getProgress, 1000);
    setInterval(getTaskState, 1000);
    setInterval(getLastResult, 2000);

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

    if (result.state === "RUNNING") {
        state.taskId = result.taskId;
        $(model.buttons.newAnalysis).prop("disabled", true);
        $(model.buttons.repairAll).prop("disabled", true);
        if (state.overlay !== 'progress' && state.overlay !== 'summary') {
            showProgressOverlay();
        }
    } else {
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

    var repairable = issuesArray.filter(function (issue) {
        return issue.repairStatus === "IS_REPAIRABLE";
    }).length;
    $(model.buttons.repairAll).prop("disabled", repairable === 0);

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
            }
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

    $(model.div.overlayRepo).text("Analysing " + info.repositoryId + " - " + info.branch);
    $(model.div.overlayValidator).text("Executing " + info.validator + " (" + info.current + " out of " + info.total + ")...");

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
    $(model.buttons.repairSummary).toggleClass("hidden", countRepairable(result) === 0);

    showOverlaySection(model.div.overlaySummary);
};

var countRepairable = function (result) {
    var count = 0;
    for (var key in result.issues) {
        if (result.issues[key].repairStatus === "IS_REPAIRABLE") {
            count++;
        }
    }
    return count;
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
