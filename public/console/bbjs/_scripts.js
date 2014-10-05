/**
 * Add ace module inline for editing
 */
/// importa ace
angular.module('BBApp', [])
    .constant('bbAceConfig', {})
    .directive('bbAce', ['bbAceConfig', function (bbAceConfig) {

        var setOptions = function (acee, session, opts) {
            if (angular.isDefined(opts.showGutter)) {
                acee.renderer.setShowGutter(opts.showGutter);
            }
            if (angular.isDefined(opts.useWrapMode)) {
                session.setUseWrapMode(opts.useWrapMode);
            }
            if (angular.isDefined(opts.showInvisibles)) {
                acee.renderer.setShowInvisibles(opts.showInvisibles);
            }
            if (angular.isDefined(opts.showIndentGuides)) {
                acee.renderer.setDisplayIndentGuides(opts.showIndentGuides);
            }
            if (angular.isDefined(opts.useSoftTabs)) {
                session.setUseSoftTabs(opts.useSoftTabs);
            }
            if (angular.isDefined(opts.disableSearch) && opts.disableSearch) {
                acee.commands.addCommands([
                    {
                        name: 'unfind',
                        bindKey: {
                            win: 'Ctrl-F',
                            mac: 'Command-F'
                        },
                        exec: function () {
                            return false;
                        },
                        readOnly: true
                    }
                ]);
            }

            // onLoad callback
            if (angular.isFunction(opts.onLoad)) {
                opts.onLoad(acee);
            }

            // Basic options
            if (angular.isString(opts.theme)) {
                acee.setTheme('ace/theme/' + opts.theme);
            }
            if (angular.isString(opts.mode)) {
                session.setMode('ace/mode/' + opts.mode);
            }
        };

        return {
            restricted: 'AE',
            require: '?ngModel',
            link: function (scope, elm, attrs, ngModel) {
                var options = bbAceConfig.ace || {};
                var opts = angular.extend({}, opts, scope.$eval(attrs.bbAce));
                var acee = window.ace.edit(elm[0]);
                var session = acee.getSession();
                var onChangeListener;
                var onBlurListener;
                var executeUserCallback = function () {
                    var callback = arguments[0];
                    var args = Array.prototype.slice.call(arguments, 1);
                    if (angular.isDefined(callback)) {
                        scope.$apply(function () {
                            if (angular.isFunction(callback)) {
                                callback(args);
                            } else {
                                throw new Error('bb-ace use a function as callback.');
                            }
                        });
                    }
                };

                var listenerFactory = {
                    onChange: function (callback) {
                        return function (e) {
                            var newValue = session.getValue();
                            if (newValue !== scope.$eval(attrs.value) && !scope.$$phase && !scope.$root.$$phase) {
                                if (angular.isDefined(ngModel)) {
                                    scope.$apply(function () {
                                        ngModel.$setViewValue(newValue);
                                    });
                                }
                                executeUserCallback(callback, e, acee);
                            }
                        };
                    },

                    onBlur: function (callback) {
                        return function () {
                            executeUserCallback(callback, acee);
                        };
                    }
                };

                attrs.$observe('readonly', function (value) {
                    acee.setReadOnly(value === 'true');
                });
                if (angular.isDefined(ngModel)) {
                    ngModel.$formatters.push(function (value) {
                        if (angular.isUndefined(value) || value === null) {
                            return '';
                        }
                        else if (angular.isObject(value) || angular.isArray(value)) {
                            throw new Error('bb-ace cannot use an object or an array as a model');
                        }
                        return value;
                    });

                    ngModel.$render = function () {
                        session.setValue(ngModel.$viewValue);
                    };
                }

                setOptions(acee, session, opts);

                scope.$watch(attrs.bbAce, function () {
                    opts = angular.extend({}, options, scope.$eval(attrs.bbAce));
                    session.removeListener('change', onChangeListener);
                    onChangeListener = listenerFactory.onChange(opts.onChange);
                    session.on('change', onChangeListener);
                    acee.removeListener('blur', onBlurListener);
                    onBlurListener = listenerFactory.onBlur(opts.onBlur);
                    acee.on('blur', onBlurListener);
                    setOptions(acee, session, opts);
                }, true);
                onChangeListener = listenerFactory.onChange(opts.onChange);
                session.on('change', onChangeListener);

                onBlurListener = listenerFactory.onBlur(opts.onBlur);
                acee.on('blur', onBlurListener);

                elm.on('$destroy', function () {
                    acee.session.$stopWorker();
                    acee.destroy();
                });

                scope.$watch(function () {
                    return [elm[0].offsetWidth, elm[0].offsetHeight];
                }, function () {
                    acee.resize();
                    acee.renderer.updateFull();
                }, true);
            }
        };
    }]);
/// ace module end


/*
 * actual ng controller and logs currenltly disabled
 */

var GLOBEVT;
function connectToScriptsLogFeed(handler) {
    var url = "/admin/stats/logs"; // cannot use absolute url for sse.
    url += '?X-BB-SESSION=' + sessionStorage.sessionToken;
    url += '&X-BAASBOX-APPCODE=' + escape($('#login').scope().appcode);
    console.log("Connecting: " + url);
    var source = new EventSource(url);
    console.log("Connected: " + source);
    source.onmessage = handler;
    GLOBEVT = source;
    return source;
}


function ScriptsController($scope) {
    var evtSource = null;
    var counter = 0;

    $scope.currentScript = null;
    $scope.data = [];

    $scope.logFilter = null;
    $scope.logEnabled = false;
    $scope.log = [];
    $scope.editTitleEnabled = false;
    $scope.tempTitle = null;


    var evtHandler = function (e) {
        $scope.$apply(function () {
            var d = e.data;
            $scope.log.push(d);
        })
    };

    // logs are currenlty disabled
    $scope.toggleLogs = function () {
        if ($scope.logEnabled) {
            $scope.logEnabled = false;
            //evtSource.close();
            //evtSource = null;
        } else {
            $scope.logEnabled = true;
            //evtSource = connectToScriptsLogFeed(evtHandler);
        }
    }

    var postScript = function (src) {
        if ($scope.editTitleEnabled) {
            $scope.setTitle();
        }
        console.log("Creating script");
        src.code.push(src.buff);
        var toSend = JSON.stringify({
            name: src.name,
            code: src.buff,
            active: true,
            lang: 'javascript'
        });
        console.log(toSend);
        BBRoutes.com.baasbox.controllers.ScriptsAdmin.create().ajax({
            data: toSend,
            contentType: "application/json",
            processData: false,
            error: function (data) {
                console.log(data);
                //todo check error upload
            },
            success: function (data) {
                console.log(data);
                //todo check done uploading
            }

        });
    };

    var updateScript = function (src) {
        if (src.code[0] === src.buff) {
            return;
        }
        console.log("Saving");
        BBRoutes.com.baasbox.controllers.ScriptsAdmin.update(src.name).ajax({
            data: JSON.stringify({code: src.buff}),
            contentType: "application/json",
            processData: false,
            error: function (data) {
                console.log("ERROR");
                console.log(JSON.stringify(data));
                //todo check error upload
            },
            success: function (data) {
                console.log("OK");
                console.log(JSON.stringify(data));
                // todo check done uploading
            }
        });
    };

    $scope.editTitle = function () {
        if ($scope.currentScript && $scope.currentScript.code.length > 0) {
            return;
        }
        $scope.tempTitle = $scope.currentScript.name;
        $scope.editTitleEnabled = true;
    }

    $scope.abortTitle = function () {
        $scope.tempTitle = null;
        $scope.editTitleEnabled = false;
    }
    $scope.setTitle = function () {
        $scope.currentScript.name = $scope.tempTitle;
        $scope.temptTitle = null;
        $scope.editTitleEnabled = false;
    }

    $scope.saveScript = function () {
        var script = $scope.currentScript;
        console.log("Saving");
        if (!script) return;
        console.log("Do save");
        if (script.code.length == 0) {
            postScript(script);
        } else {
            updateScript(script);
        }
    }

    // creates a new unbounded script
    // with no name and empty code;
    $scope.createNew = function () {
        $scope.abortTitle();
        var newScript = {
            name: 'untitled_' + counter,
            lang: 'javascript',
            code: [],
            buff: '// untitled',
            active: true
        };
        counter++;
        $scope.data.push(newScript);
    };

    $scope.setScript = function (idx) {
        $scope.abortTitle();
        var script = $scope.data[idx];
        script.buff = script.buff || script.code[0];
        $scope.currentScript = script;
    }
}

