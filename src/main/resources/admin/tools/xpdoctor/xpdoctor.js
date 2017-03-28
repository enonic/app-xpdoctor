var thymeleaf = require('/lib/xp/thymeleaf');
var portal = require('/lib/xp/portal');
var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var view = resolve('xpdoctor.html');

    var result = dataValidator.execute("cms-repo", "master");

    var model = {
        result: JSON.stringify(result, null, 4),
        jsUrl: portal.assetUrl({path: "/js/main.js"}),
        assetsUrl: portal.assetUrl({path: ""}),
        repoLoaderServiceUrl: getServiceUrl('repo-loader-service')
    };

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, model)
    };

};

var getServiceUrl = function (name) {

    return portal.serviceUrl({
        service: name
    })
};
