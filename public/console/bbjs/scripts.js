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
	var EXTRACT_ERROR = /ScriptError: '([^]*?)at jdk\.nashorn/m;

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
			setEditMode(true);
			$scope.showStorage=false;
		} else {
			prompt("Script name "+resp+ " is not valid, choose another one","").then(onNewScript,noop);
		}
	};

	var setStorageFormatted = function(){
		if($scope.currentScript && $scope.currentScript._storage){
			return angular.toJson($scope.currentScript._storage,true);
		} else{
			return "";
		}
	};

	var onUpdateSucces = function(){
		$scope.currentScript.buffer = undefined;
		$scope.currentScript = null;
		$scope.selected= -1;
		setEditMode(false);
		$scope.showStorage=false;
		$scope.reload();
	};


	var setEditMode = function(mode){
		$scope.editMode = mode;
		$scope.aceEditMode.theme = mode?'solarized_dark':'crimson_editor';
	};

	var parseError = function(text){
		var x =EXTRACT_ERROR.exec(text);
		if(x && x[1]){
			console.log(x[1]);
			return x[1];
		}
		return "Unknown error";
	};

	var pubblishError = function(data){
		var e = $.parseJSON(data.responseText);
		var error= parseError(e.message);
		$scope.$apply(function(){
			$scope.lastError = error;
		});
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

			error: pubblishError,
			success: onUpdateSucces
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
			error: pubblishError,
			success: onUpdateSucces
		});

	};

	$scope.aceEditMode = {mode: 'javascript',
						  theme: 'crimson_editor',
						  require: ['ace/ext/language_tools'],
						  advanced:{enableSnippets: true,
							  		enableBasicAutocompletion: true,
							  		enableLiveAutocompletion: true}};
	$scope.data={};
	$scope.currentScript = null;
	$scope.showStorage=false;
	$scope.selected = -1;
	$scope.editMode = false;
	$scope.lastError = null;
	$scope.storageFormatted="";

	$scope.logEnabled = false;
	$scope.logs = [];
	$scope.maxLogSize = 40;


	$scope.newScript = function(){
		prompt("Script name","").then(
			onNewScript,
			noop);
	};

	$scope.toggleEdit = function(){
		setEditMode(!$scope.editMode);

	};

	$scope.toggleStorageView=function(){
		$scope.showStorage = !$scope.showStorage;
		if($scope.showStorage){
			$scope.storageFormatted = setStorageFormatted();
		}
	};

	$scope.selectItem = function(index){
		$scope.selected=index;
		setEditMode(false);
		$scope.showStorage=false;
		var scr = $scope.data.data[index];
		scr.buffer = scr.buffer||scr.code[0];
		$scope.currentScript = scr;
	};


	$scope.closeEditor = function(){
		if($scope.currentScript) {
			$scope.currentScript.buffer = undefined;
		}
		$scope.currentScript = null;
		setEditMode(false);
		$scope.showStorage=false;
		$scope.selected=-1;
		$scope.lastError=null;
	};

	$scope.saveScript = function(){
		if(!$scope.currentScript) return;
		if($scope.currentScript.code){
			updateScript($scope.currentScript);
		} else{
			postScript($scope.currentScript);
		}
	};


	///---- menu functions
	$scope.reload=function(){
		BBRoutes.com.baasbox.controllers.ScriptsAdmin.list().ajax({
			success: function(data) {
				$scope.$apply(function(){
					$scope.data=data;
					$scope.closeEditor();

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


	///---- logging system

	var evtSource = null;

	var connectLogger = function(f){
		var url = "/admin/plugin/logs";
		url+='?X-BB-SESSION=' + sessionStorage.sessionToken;
		url+='&X-BAASBOX-APPCODE='+escape($('#login').scope().appcode);
		var source = new EventSource(url);
		source.addEventListener('message',f);
		return source;
	};

	$scope.toggleLogs = function(){
		if($scope.logEnabled) {
			$scope.logEnabled = false;
			evtSource.close();
			evtSource =null;
			$scope.logs.splice(0,$scope.logs.length);
		} else{
			$scope.logEnabled = true;
			evtSource = connectLogger(function(e){
				//console.log(e.data);
				var temp = e.data;//.split('\n').join('');
				temp=temp.replace(/\\"/g,'"');
				console.log(temp);
				var data = JSON.parse(temp);


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