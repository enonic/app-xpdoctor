var thymeleaf = require('/lib/thymeleaf');
var portal = require('/lib/xp/portal');
var assetLib = require('/lib/enonic/asset');
var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var view = resolve('xpdoctor.html');

    var wsUrl = portal.serviceUrl({service: 'event-bus', type: 'absolute'})
    wsUrl = 'ws' + wsUrl.substring(wsUrl.indexOf(':'));

    var model = {
        validators: createValidatorsModel(),
        faviconUrl: assetLib.assetUrl({path: 'images/favicon-48x48.png'}),
        resetCssUrl: assetLib.assetUrl({path: 'css/reset.css'}),
        mainCssUrl: assetLib.assetUrl({path: 'css/main.css'}),
        jqueryUrl: assetLib.assetUrl({path: 'js/jquery-3.1.1.min.js'}),
        renderJsonUrl: assetLib.assetUrl({path: 'js/renderjson.js'}),
        mainJsUrl: assetLib.assetUrl({path: 'js/main.js'}),
        dataTablesJsUrl: assetLib.assetUrl({path: 'js/jquery.dataTables.min.js'}),
        dataTablesCssUrl: assetLib.assetUrl({path: 'css/jquery.dataTables.min.css'}),
        logoUrl: assetLib.assetUrl({path: 'images/xpDoc.png'}),
        validatorServiceUrl: getServiceUrl('validator-service'),
        progressServiceUrl: getServiceUrl('progress-service'),
        stateServiceUrl: getServiceUrl('state-service'),
        lastResultServiceUrl: getServiceUrl('last-result-service'),
        repairServiceUrl: getServiceUrl('repair-service'),
        repoListServiceUrl: getServiceUrl('repo-list-service'),
        wsUrl: wsUrl
    };

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, model)
    };

};

var createValidatorsModel = function () {
    return dataValidator.validators().validators;
};

var getServiceUrl = function (name) {

    return portal.serviceUrl({
        service: name
    })
};
