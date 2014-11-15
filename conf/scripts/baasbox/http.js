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


var PATH_REGEX = new RegExp(['(\\\\.)',
    '([\\/.])?(?:\\:(\\w+)(?:\\(((?:\\\\.|[^)])*)\\))?|\\(((?:\\\\.|[^)])*)\\))([+*?])?',
    '([.+*?=^!:${}()[\\]|\\/])'].join('|'),'g');

function escapeGroup(group){
    return group.replace(/([=!:$(\/()])/g,'\\$1');
}

var attachKeys = function(re, keys){
    re.keys = keys;
    return re;
};

function pathToRegexp(path,keys,options) {
    if(keys && !Array.isArray(keys)){
        options = keys;
        keys = null;
    }

    keys = keys || [];
    options = options || {};
    var strict = options.strict;
    var end = options.end !== false;
    var flags = options.sensitive ? '' : 'i';
    var index = 0;

    if (path instanceof RegExp) {
        var groups = path.source.match(/\((?!\?)/g) || []
        keys.push.apply(keys,groups.map(function(match,index){
            return {
                name: index,
                delimiter: null,
                optional: false,
                repeat: false
            }
        }));

        return attachKeys(path,keys);
    }

    if(Array.isArray(path)){
        path = path.map(function(value){
            return pathToRegexp(value,keys,options).source;
        });
        return attachKeys(new RegExp('(?:'+path.join('|')+')',flags),keys);
    }

    path = path.replace(PATH_REGEX,function(match,escaped,prefix,key,capture,group,suffix,escape){
        if(escaped) {
            return escaped;
        }
        if(escape){
            return '\\'+escape;
        }
        var repeat = suffix === '+' || suffix === '*';
        var optional = suffix === '?'|| suffix === '*';
        keys.push({
            name: key||index++,
            delimiter: prefix||'/',
            optional: optional,
            repeat: repeat
        });

        prefix = prefix? '\\'+prefix:'';

        capture = escapeGroup(capture || group || '[^' + (prefix || '\\/') + ']+?');

        if (repeat) {
            capture = capture + '(?:' + prefix + capture + ')*';
        }

        if (optional) {
            return '(?:' + prefix + '(' + capture + '))?';
        }

        return prefix + '(' + capture + ')';
    });

    var endsWithSlash = path[path.length - 1] === '/';
    if (!strict) {
        path = (endsWithSlash ? path.slice(0, -2) : path) + '(?:\\/(?=$))?';
    }
    if (!end) {
        path += strict && endsWithSlash ? '' : '(?=\\/|$)';
    }
    return attachKeys(new RegExp('^' + path + (end ? '$' : ''), flags), keys);
}





function HttpServer(){
    this.handlers = {get: [],post: [],put: [],delete: []};
};

function Binding(method,route,fn){
    this.method = method;
    this.regex = regex;
    this.keys = keys;
}

HttpServer.prototype.get = function(route,fn){
    var b = new Binding('get',route,fn);
    this.handlers.get.push(b);
    return this;
};

HttpServer.prototype.put = function(route,fn){
    var b = new Binding('put',route,fn);
    this.handlers.put.push(b);
    return this;
};

HttpServer.prototype.post = function(route,fn){
    var b  = new Binding('post',route,fn);
    this.handlers.post.push(b);
    return this;
};

HttpServer.prototype.delete = function(route,fn){
    var b = new Binding('delete',route,fn);
    this.handlers.delete.push(b);
    return this;
};

HttpServer.prototype.all = function(route,fn){
    this.get(route,fn).post(route,fn).put(route,fn).delete(route,fn);
    return this;
};

function findBinding(bindings,req){
    var i;
    var b = null;
    var m = null;
    for(i = 0;i<bindings.length;i++){
        b = bindings[i];
        m = b.matches(req.path);
        if (m) return m;
    }
    return null;
}

HttpServer.prototype.handle = function(){
    var self = this;

    return new function(event){
        if(event.name !== 'request') throw new Error('http handler can only be used to serve http')
        var req = event.data;
        var hlist = self.handlers[req.method];
        var binding = findBinding(req);


    };
}



//exports.serve = function(){
//    return function(){
//        var inst = function(){
//            return inst.serve.apply(inst,arguments);
//        };
//        HttpServer.apply(inst,arguments);
//        return inst;
//    };
//    //callableType(HttpServer);
//};

//exports.client = function(){};


