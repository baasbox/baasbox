function loadScriptsPage(scopeName){
   	//load scripts
	BBRoutes.com.baasbox.controllers.Admin.getConfiguration("Push").ajax({
		success: function(data) {
			applySuccessMenu(scopeName,data);
		}
	});
}


function ScriptController($scope){
	var _this = $scope;
	_this.data={};
	_this.data.isLoaded=false;
}


