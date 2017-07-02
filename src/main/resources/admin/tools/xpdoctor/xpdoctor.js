var thymeleaf = require('/lib/xp/thymeleaf');
var portal = require('/lib/xp/portal');
var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var view = resolve('xpdoctor.html');

    var result = dataValidator.execute();

    var model = {
        result: JSON.stringify(result, null, 4),
        jsUrl: portal.assetUrl({path: "/js/main.js"}),
        assetsUrl: portal.assetUrl({path: ""}),
        repoLoaderServiceUrl: getServiceUrl('repo-loader-service'),
        uniquePathValidatorUrl: getServiceUrl('unique-path-validator')
    };


    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, model)
    };

};

var validate = function () {

    var targets = [
        {
            repoId: "cms-repo",
            branches: ["draft, master"]
        }, {
            repoId: "system-repo",
            branches: ["master"]
        }];

    var result = dataValidator.execute("cms-repo", "master");
};

var getServiceUrl = function (name) {

    return portal.serviceUrl({
        service: name
    })
};
