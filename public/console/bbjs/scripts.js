function loadScriptsPage(scopeName){
   	//load scripts
	
	BBRoutes.com.baasbox.controllers.ScriptsAdmin.list().ajax({
		success: function(data) {
			applySuccessMenu(scopeName,data);
		}
	});
}


function ScriptsController($scope){
	var _this = $scope;
}

angular.module('console', [])
	.directive('jsCodeHighlight', function($rootScope){
	    return {
	        restrict: 'A',
	        scope:false,
	        link: function(scope,elm,attrs){
	        	elm.html(scope.script.code[0]);
	            elm.snippet("javascript",{style:"whitengrey"});
	        }
	    }
	});
