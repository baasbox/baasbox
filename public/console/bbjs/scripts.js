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
	var VALID_NAME = /^([a-z_][a-z_0-9]*)(\.[a-z_][a-z_0-9]*)+$/i;
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
			$scope.currentScript ={buffer: "/* script: "+resp+" */\n", name: resp};
			$scope.selected=-1;
			$scope.editMode=true;
			$scope.showStorage=false;
		} else {
			prompt("Script name "+resp+ " is not valid, choose another one","").then(onNewScript,noop);
		}
	};

	var setStorageFormatted = function(){
		if($scope.currentScript && $scope.currentScript._storage){
			return angular.toJson($scope.currentScript._storage,true);
			//return JSON.stringify($scope.currentScript._storage);
		} else{
			return "";
		}
	};

	var onUpdateSucces = function(){
		$scope.currentScript.buffer = undefined;
		$scope.currentScript = null;
		$scope.selected= -1;
		$scope.editMode=false;
		$scope.showStorage=false;
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


	$scope.data={};
	$scope.currentScript = null;
	$scope.showStorage=false;
	$scope.selected = -1;
	$scope.editMode = false;

	$scope.newScript = function(){
		prompt("Script name","").then(
			onNewScript,
			noop);
	};

	$scope.storageFormatted="";

	$scope.toggleEdit = function(){
		$scope.editMode = !$scope.editMode;
	};


	$scope.toggleStorageView=function(){
		$scope.showStorage = !$scope.showStorage;
		if($scope.showStorage){
			$scope.storageFormatted = setStorageFormatted();
		}
	}

	$scope.selectItem = function(index){
		$scope.selected=index;
		$scope.editMode=false;
		$scope.showStorage=false;
		var scr = $scope.data.data[index];
		scr.buffer = scr.buffer||scr.code[0];
		$scope.currentScript = scr;
		//$scope.storageFormatted = setStorageFormatted();
	};


	$scope.closeEditor = function(){
		$scope.currentScript.buffer = undefined;
		$scope.currentScript = null;
		$scope.editMode=false;
		$scope.showStorage=false;
		$scope.selected=-1;
	};

	//$scope.getSelectedItem = function(){
	//	return $scope.selected;
	//};


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
					$scope.selected=-1;
					if($scope.currentScript){
						$scope.currentScript.buffer = undefined;
					}
					$scope.currentScript = undefined;
					$scope.editMode = false;
					$scope.showStorage = false;
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


	/// logging

	var evtSource = null;
	var connectLogger = function(f){
		var url = "/admin/plugin/logs";
		url+='?X-BB-SESSION=' + sessionStorage.sessionToken;
		url+='&X-BAASBOX-APPCODE='+escape($('#login').scope().appcode);
		var source = new EventSource(url);
		source.addEventListener('message',f);
		return source;
	};

	$scope.logEnabled = false;
	$scope.logs = [];
	$scope.maxLogSize = 40;

	$scope.toggleLogs = function(){
		if($scope.logEnabled) {
			$scope.logEnabled = false;
			evtSource.close();
			evtSource =null;
			$scope.logs.splice(0,$scope.logs.length);
		} else{
			$scope.logEnabled = true;
			evtSource = connectLogger(function(e){
				console.log(e.data);
				var data = JSON.parse(e.data.split('\\').join(''));


				$scope.$apply(function(){
					console.log(data);
					$scope.logs.unshift(data);
					if($scope.logs.length<$scope.maxLogSize) return;
					Array.prototype.splice.call($scope.logs,$scope.maxLogSize,($scope.logs.length-$scope.maxLogSize));
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