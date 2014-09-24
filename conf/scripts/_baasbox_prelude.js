/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by Andrea Tortorella on 10/06/14.
 */

var GLOBAL=this;

(function(){
    var root = {};

    var Internal = com.baasbox.service.scripting.js.Internal;
    var Api = com.baasbox.service.scripting.js.Api;
    var JsJsonCallback = com.baasbox.service.scripting.js.JsJsonCallback;

    Internal.log("Start evaluating prelude")

    // custom baasbox error
    function BaasBoxError(data){
        this.name = "BaasboxError";
        if(typeof data === 'string'){
            this.message=data;
        } else if(typeof data === 'object'){
            this.data=data;
            this.message=data.message||"baasbox generic error";
        }
    }
    BaasBoxError.prototype=Object.create(Error.prototype);
    BaasBoxError.prototype.constructor =BaasBoxError;

    function Storage(mod){
        this.mod = mod;
    }

    Storage.prototype.get = function(){
        return this.mod._command({resource: 'script',
                                  name: 'storage',
                                  params: { action: 'get'}})
    };
    Storage.prototype.set = function(o) {
        return this.mod._command({resource: 'script',
                                  name: 'storage',
                                  params: {action: 'set',
                                           args: o}
        });
    };
    Storage.prototype.swap = function(f) {
        return this.mod._command({resource: 'script',
                                  name: 'storage',
                                  callback: f,
                                  params: {action: 'swap'}});
    };
    Storage.prototype.swap = function(f) {
        return this.mod._command({resource: 'script',
            name: 'storage',
            callback: f,
            params: {action: 'trade'}});
    };

    /**
     * A Module as seen from javascript
     * @param name
     * @constructor
     */
    function Module(name) {
        /**
         * The module id;
         */
        this.id = name;

        /**
         * Exported objects of the module
         * @type {null}
         */
        this.exports=Object.create(null);
        var m = this;

        this._command = function(command) {
            if (!(typeof command === 'object')) {
                throw new TypeError("command must be an 'object'");
            }
            try {
                command.mod = m.id;
                var cb =(typeof command.callback === 'function')
                            ? new JsJsonCallback(command.callback,m,JSON)
                            : null;
                Internal.log("callback: "+ (cb != null));
                var resp = Api.execCommand(JSON.stringify(command),cb);
                if (resp == null) {
                    return null;
                }
                return JSON.parse(resp);
            } catch (e){
                Internal.log(e,"msg");
                throw new BaasBoxError(JSON.parse(e));
            }
        }

        Object.defineProperty(this,"storage",{value: new Storage(m),
                                              configurable: false,
                                              enumerable: false});
        /**
         * Context property
         */
        Object.defineProperty(this,"context",
            {value:
                Object.create(Object.prototype,{
                      "userName":
                      {get: function(){
                                return Api.currentUserName();
                            },
                      enumerable: true,
                      configurable: false},

                      "main":
                      {get: function(){
                                return Api.mainModule();
                            },
                       enumerable: true,
                       configurable: false}}),
             enumerable: false,
             configurable: false
            });

    }

    ["Infinity","NaN","undefined","eval","isFinite","isNaN","parseFloat",
     "parseInt","decodeURI","decodeURIComponent","encodeURI","encodeURIComponent",
     "Object","Function","Boolean","Error","EvalError","RangeError","ReferenceError",
     "SyntaxError","TypeError","URIError","Number","Math","Date","String","RegExp","Array","Float32Array",
     "Float64Array","Int16Array","Int32Array","Int8Array","Uint8Array","Uint16Array","Uint32Array",
     "Uint8Array","ArrayBuffer","JSON"].forEach(function (x) {
        Object.defineProperty(Module.prototype,x,{value: GLOBAL[x],enumerable: false, configurable: false});
     });

    Module.prototype.BaasBoxError=BaasBoxError;


    /**
     * The require function
     * allow to load new modules
     * @param name
     */
    Module.prototype.require = function(name){
        if (typeof name !== 'string'){
            throw new TypeError("require needs a single string argument");
        }
        var mod =Api.require(name);
        if(mod === null){
            throw new Error("module "+name+"does not exists");
        }
        return mod.module.exports;
    };
    Object.defineProperty(Module.prototype,"require",{configurable: false,enumerable: false});

//    we should remove this in favor of better logging mechaninsm
    Module.prototype.Console = Object.create({
        log: function(val) {
            Internal.log(JSON.stringify(val));
        }
    });

    function ModuleRef(id,code) {
        this.dispathTable= {};
        this.code=code;
        this.id = id;

        var ref = this;
        this.module = new Module(id);

        Object.defineProperty(this.module,"id",{configurable: false,writable: false});
        Object.defineProperty(this.module,"_command",{configurable: false,writable: false});
        Object.defineProperty(this.module,"on",
            {configurable: false,
             writable: false,
             enumerable: false,
             value: function(evt,handler){
                 Internal.log("Binding event "+evt);
                 if(! typeof handler === 'function'||
                   ! typeof handler === 'undefined'){
                   throw new TypeError('handler must be a function');
                 }
                   ref.dispathTable[evt]=handler;
                 }
        });

    }

    /**
     * Used from java:
     * Invokes an event
     * @param evt  an event has the shape {name: 'name of the event',data: *}
     * @returns {*}
     */
    ModuleRef.prototype.emit = function(evt){
        var handler = this.dispathTable[evt.name];
        var wrapper;
        if (evt === 'request') {
            wrapper = function(e){
                if (handler !== undefined) {
                    return handler(e);
                } else{
                    return {status: 404, content: "not found"};
                }
            }
        } else {
           wrapper = handler;
        }

        if (wrapper !== undefined) {

            return wrapper.apply(this.module, [evt]);

        }
    };

    /**
     * Compile is called from java to evaluate a script
     * The script is evaluated in the context of this.module.
     *
     * @returns {ModuleRef}
     */
    ModuleRef.prototype.compile = function(){
        load.apply(this.module,[{name: this.id,script: this.code}]);
        Object.freeze(this.module.exports); //exports are frozen after compilation
        return this;
    };

    // bindings for java
    root.makeModule =  function(id,code){
        return new ModuleRef(id,code);
    };

    root.asJson = function(arg) {
        Internal.log("TYPE: "+(typeof arg));
        return JSON.parse(arg);
    };

    root.toJsonString= function(arg){
        Internal.log("Type: "+(typeof arg));
        return JSON.stringify(arg);
    }

    Internal.log("Prelude evaluation completed");
    return root;
}());