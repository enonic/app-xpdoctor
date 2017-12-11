var thymeleaf = require('/lib/xp/thymeleaf');
var portal = require('/lib/xp/portal');
var dataValidator = require('/lib/dataValidator.js');

exports.get = function (req) {

    var view = resolve('xpdoctor.html');

    var wsUrl = portal.serviceUrl({service: 'event-bus', type: 'absolute'})
    wsUrl = 'ws' + wsUrl.substring(wsUrl.indexOf(':'));

    var model = {
        validators: createValidatorsModel(),
        assetsUrl: portal.assetUrl({path: ""}),
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
