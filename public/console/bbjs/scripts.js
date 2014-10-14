function loadScriptsPage(scopeName){
   	//load scripts
	
	BBRoutes.com.baasbox.controllers.ScriptsAdmin.list().ajax({
		success: function(data) {
			applySuccessMenu(scopeName,data);
		}
	});
}


function ScriptsController($scope,prompt){
	// private helpers
	var VALID_NAME = /^([a-z][a-z0-9]*)\.[a-z][a-z0-9]*$/i;
	var noop = function(){};
	var validateName = function(name){
		var r = VALID_NAME.exec(name);
		var idx;
		if(r&& r[1] !== 'baasbox'){
			var ary = $scope.data.data;
			var l = ary.length;
			for(idx = 0;idx<l;idx++){
				if(ary[idx].name == name){
					return false;
				}
			}
			return true;
		} else{
			return false;
		}
	};

	var onNewScript = function(resp){
		if(validateName(resp)){
			$scope.currentScript ={buffer: "/* script: "+resp+" */\n"};
		} else {
			prompt("Script name "+resp+ " is not valid, choose another one","").then(onNewScript,noop);
		}
	};

	var onUpdateSucces = function(){
		$scope.currentScript.buffer = undefined;
		$scope.currentScript = null;
		$scope.selected= 0;
		$scope.reload();
	};

	var postScript = function(script){
		var toSend = JSON.stringify({
			name: script.name,
			code: script.buffer,
			active: true,
			lang: 'javascript'
		});
		BBRoutes.com.baasbox.controllers.ScriptsAdmin.create().ajax({
			data: toSend,
			contentType: "application/json",
			processData: false,

			error: function(data){
				console.log(data);
			},
			success: function(data){
				onUpdateSucces();
			}
		});
	};

	var updateScript = function(script){
		var toSend = JSON.stringify({
			code: script.buffer
		});
		BBRoutes.com.baasbox.controllers.ScriptsAdmin.update(script.name).ajax({
			data: toSend,
			contentType: "application/json",
			processData: false,
			error: function(data){
				console.log(data);
			},
			success: function(data){
				onUpdateSucces();
			}
		});

	};

	$scope.selected=0;
	$scope.data={};
	$scope.currentScript = null;
	$scope.showStorage=false;

	$scope.newScript = function(){
		prompt("Script name","").then(
			onNewScript,
			noop);
	};


	$scope.edit = function(){
		var scr = $scope.data.data[$scope.selected];
		scr.buffer = scr.buffer||scr.code[0];
		$scope.currentScript = scr;
	};

	$scope.discardEdits = function(){
		var scr = $scope.currentScript;
		if (scr.code){
			scr.buffer=scr.code[0];
		} else {
			scr.buffer = "/* script: "+scr.name+" */\n";
		}
		$scope.currentScript = scr;

	};

	$scope.selectItem = function(s){
		$scope.selected=s;
	};

	$scope.closeEditor = function(){
		$scope.currentScript.buffer = undefined;
		$scope.currentScript = null;
	};

	$scope.getSelectedItem = function(){
		return $scope.selected;
	};


	$scope.saveScript = function(){
		if(!$scope.currentScript) return;
		if($scope.currentScript.code){
			updateScript($scope.currentScript);
		} else{
			postScript($scope.currentScript);
		}
	};
	
	$scope.reload=function(){
		BBRoutes.com.baasbox.controllers.ScriptsAdmin.list().ajax({
			success: function(data) {
				$scope.$apply(function(){
					$scope.data=data;
					$scope.selected=0;
				});
			}
		});
	};
	
	$scope.activate=function(index){
		var name=$scope.data.data[index].name;
		BBRoutes.com.baasbox.controllers.ScriptsAdmin.activate(name).ajax({
			success: function(data) {
				$scope.reload();
			}
		});
	}//$scope.activate()

	$scope.deactivate=function(index){
		var name=$scope.data.data[index].name;
		BBRoutes.com.baasbox.controllers.ScriptsAdmin.deactivate(name).ajax({
			success: function(data) {
				$scope.reload();
			}
		});
	}//$scope.deactivate()
	
	$scope.remove=function(index){
		var name=$scope.data.data[index].name;
		if (confirm("This will permanently delete the plugin from the server. Are you sure?")){
			BBRoutes.com.baasbox.controllers.ScriptsAdmin.drop(name).ajax({
				success: function(data) {
					$scope.reload();
				}
			});
		}
	}//$scope.remove()
	
	$scope.toggleStorageView=function(){
		$scope.showStorage = !$scope.showStorage;
	}
	
	$scope.getShowStorage=function(){
		return $scope.showStorage;
	}

	var evtSource = null;
	var connectLogger = function(f){
		var url = "/admin/plugin/logs";
		url+='?X-BB-SESSION=' + sessionStorage.sessionToken;
		url+='&X-BAASBOX-APPCODE='+escape($('#login').scope().appcode);
		console.log("Connecting: "+url);
		var source = new EventSource(url);
		console.log("Connected: "+source);
		source.addEventListener('message',f);
		return source;
	};

	$scope.logEnabled = false;
	$scope.logs = [];

	$scope.toggleLogs = function(){
		if($scope.logEnabled) {
			$scope.logEnabled = false;
			evtSource.close();
			evtSource =null;
		} else{
			$scope.logEnabled = true;
			evtSource = connectLogger(function(e){
				console.log(e.data);
				var data = JSON.parse(e.data.split('\\').join(''));


				$scope.$apply(function(){
					console.log(data);
					$scope.logs.push(data);
				})
			});
		}
	}
	
}

/*
angular.module('console', [])
	.directive('jsCodeHighlight', function($rootScope){
	    return {
	        restrict: 'A',
	        scope:false,
	        link: function(scope,elm,attrs){
	        	console.log("direttiva!");
	        	//elm.html(scope.script.code[0]);
	            elm.snippet("javascript",{style:"whitengrey"});
	        }
	    }
	});
*/



angular.module('console', [])
	.directive('snippet', ['$timeout', '$interpolate', function($timeout, $interpolate) {
		return {
	        restrict: 'E',
	        template:'<pre><code ng-transclude></code></pre>',
	        replace:true,
	        transclude:true,
	        link:function(scope, elm, attrs){             
	            var tmp =  $interpolate(elm.find('code').text())(scope);
	             $timeout(function() {                
	                elm.find('code').html(hljs.highlightAuto(tmp).value);
	              }, 0);
	        }
	    };
	}]);