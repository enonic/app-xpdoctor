var thymeleaf = require('/lib/thymeleaf');
var portal = require('/lib/xp/portal');
var admin = require('/lib/xp/admin');
var assetLib = require('/lib/enonic/asset');
var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var view = resolve('xpdoctor.html');

    var wsUrl = portal.apiUrl({api: 'event-bus', type: 'websocket'});

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
        validatorApiUrl: getApiUrl('validator'),
        progressApiUrl: getApiUrl('progress'),
        stateApiUrl: getApiUrl('state'),
        lastResultApiUrl: getApiUrl('last-result'),
        repairApiUrl: getApiUrl('repair'),
        repoListApiUrl: getApiUrl('repo-list'),
        cancelApiUrl: getApiUrl('cancel'),
        wsUrl: wsUrl,
        menuLoaderUrl: admin.extensionUrl({
            application: 'com.enonic.xp.app.main',
            extension: 'menu-loader',
            params: {
                appName: app.name,
                theme: 'light'
            }
        })
    };

    return {
        contentType: 'text/html',
        body: thymeleaf.render(view, model)
    };

};

var createValidatorsModel = function () {
    return dataValidator.validators().validators.map(function (validator) {
        validator.title = validator.name.replace(/Validator$/, '').replace(/([a-z])([A-Z])/g, '$1 $2');
        return validator;
    });
};

var getApiUrl = function (name) {

    return portal.apiUrl({
        api: name
    })
};
