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
        statusServiceUrl: getServiceUrl('status-service'),
        stateServiceUrl: getServiceUrl('state-service'),
        lastResultServiceUrl: getServiceUrl('last-result-service'),
        wsUrl: wsUrl
    };

    log.info("Model: %s", JSON.stringify(model, null, 4));

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
