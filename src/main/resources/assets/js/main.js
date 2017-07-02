var model = {
    selectors: {}
};

$(function () {
    renderjson.set_show_to_level("all");
    var renderedHtml = renderjson(JSON.parse(validatorResult));

    console.log("HTML", renderedHtml);
    $('#result').append(renderedHtml);
    // initializeModel();
    //getRepoList(model.selectors.repo);
});

var initializeModel = function () {
    model.selectors.repo = $('#selectRepoId');
};

var getRepoList = function (renderer) {

    jQuery.ajax({
        url: repoLoaderServiceUrl,
        cache: false,
        type: 'GET',
        success: function (result) {
            renderRepoList(result, renderer);
        }
    });
};

var renderRepoList = function (result, renderer) {

    var html = "";
    html += '<option value="" disabled selected>Select repository</option>';
    result.repoList.forEach(function (entry) {
        html += "<option value='" + entry.id + "'>" + entry.id + "</option>";
    });

    renderer.html(html);
};
