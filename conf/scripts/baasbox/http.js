/**
 * Created by eto on 23/09/14.
 */

var callableType = function (constructor) {
    return function () {
        var callableInstance = function () {
            return callableInstance.callOverload.apply(callableInstance, arguments);
        };
        constructor.apply(callableInstance, arguments);
        return callableInstance;
    };
};

var HttpServer = function(){
    this.serve = function(e){
        //todo handle http request
    };
    this.callOverload = serve;
};


exports.serve = function(){
    return function(){
        var inst = function(){
            return inst.serve.apply(inst,arguments);
        };
        HttpServer.apply(inst,arguments);
        return inst;
    };
    //callableType(HttpServer);
};

//exports.client = function(){};


