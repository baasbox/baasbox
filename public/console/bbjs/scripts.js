function loadScriptsPage(scopeName){
   	//load scripts
	
	BBRoutes.com.baasbox.controllers.ScriptsAdmin.list().ajax({
		success: function(data) {
			applySuccessMenu(scopeName,data);
		}
	});
}


function ScriptsController($scope){
	$scope.selected=0;
	$scope.data={};
	$scope.showStorage=false;
	
	$scope.selectItem = function(s){
		$scope.selected=s;
	}
	
	$scope.getSelectedItem = function(){
		return $scope.selected;
	}
	
	$scope.reload=function(){
		BBRoutes.com.baasbox.controllers.ScriptsAdmin.list().ajax({
			success: function(data) {
				$scope.$apply(function(){
					$scope.data=data;
					$scope.selected=0;
				});
			}
		});
	}
	
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

angular.module("console", ['ui.ace']);

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