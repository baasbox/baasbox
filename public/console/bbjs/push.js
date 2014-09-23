function loadPushSettings(scopeName){
   	//load push settings
	BBRoutes.com.baasbox.controllers.Admin.getConfiguration("Push").ajax({
		success: function(data) {
			//we have to reorganize the settings of the Push to better usage 
			console.debug("dumpConfiguration Push success:");
			settingPushDataArray = data["data"];
			settingPushMap = {};
			$(settingPushDataArray).each(function(i,setting){
				var k = setting["key"];
				if(k.endsWith(".certificate")){
					setting["file"] = true
					if(setting.value){
						setting["filename"] = JSON.parse(setting.value).name
					}
				}else{
					setting["file"] = false;
				}
				settingPushMap[k]=setting;
			});
			console.log(settingPushMap);
			settingPushMap.isLoaded=true;
			applySuccessMenu(scopeName,settingPushMap);
		}
	});
}


function PushConfController($scope){
	var _this = $scope;
	_this.data={};
	_this.data.isLoaded=false;
	
	
	_this.getProfileName = function(profileNumber) {
		if (profileNumber==1) profile="profile1";
		else profile="profile"+profileNumber;
		return profile;
	}
	
	_this.booleanValue=function(setting){
		if (setting.type=="Boolean"){
			if (setting.value==null) return false;
			return setting.value=="true";
		}else throw Exception (setting.key + " is not a boolean");
	}
	
	_this.isEnabled=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.booleanValue(_this.data[profile+".push.profile.enable"]);
		else return null;
	}
	
	_this.isSandbox=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.booleanValue(_this.data[profile+".push.sandbox.enable"]);
		else return false;
	}
	
	_this.getEnableKey=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".push.profile.enable"];
		else return null;
	}
	
	_this.getSandboxKey=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".push.sandbox.enable"];
		else return null;
	}
	
	_this.getProductionAndroidKey=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".production.android.api.key"];
		else return null;
	}
	
	_this.getProductionIOSCertificate=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".production.ios.certificate"];
		else return null;
	}
	
	_this.getProductionIOSCertificatePassword=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".production.ios.certificate.password"];
		else return null;
	}
	_this.getIOSTimeout=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".push.apple.timeout"];
		else return null;
	}
	_this.getSandboxAndroidKey=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".sandbox.android.api.key"];
		else return null;
	}
	
	_this.getSandboxIOSCertificate=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".sandbox.ios.certificate"];
		else return null;
	}
	
	_this.getSandboxIOSCertificatePassword=function(profileNumber){
		var profile=_this.getProfileName(profileNumber)
		if (_this.data.isLoaded)
			return _this.data[profile+".sandbox.ios.certificate.password"];
		else return null;
	}
	
	$scope.valueChanged = function(s){
		_this.$apply(function(scope){
			s.changed=true;
		});
	}

	$scope.isChanged = function(s){
		if (s) return s.changed
		return false;
	}
	
	$scope.updateInlineSetting = function(s,newValue){
		var section="Push";
		newValue=newValue || s.value
		//console.debug(s.value)
		s.error = null;
		if(newValue.trim()===""){
			s.error = "Value can't be empty";
			return;
		}
		var ajaxProps = BBRoutes.com.baasbox.controllers.Admin.setConfiguration(section,"dummy",s.key, '');
		var url = ajaxProps.url.substr(0,ajaxProps.url.length-1).replace('/dummy/','/')
		$.ajax(
				{
					method:'PUT',
					type:'PUT',
					url:url,
					contentType:'application/json',
					url:url,
					data:JSON.stringify({value:newValue}),
					error: function(data)
					{
						////console.debug(data)
						jsonResponse=JSON.parse(data.responseText);
						alert("Error updating settings:" + jsonResponse["message"]);
						_this.$apply(function(scope){
							s.error = jsonResponse["message"];
						});
					},
					success: function(data)
					{
						//alert("Setting "+s.key+" saved succesfully")
						_this.$apply(function(scope){
							_this.data[s.key].value=newValue;
							s.changed = false;
						});	
					}
				});
	}//updateInlineSetting
	
	$scope.updateFileSetting = function(s){
		var section="Push";
		//console.debug("s",$scope[s.key])
		s.error = null;
		if($scope.file==null){
			s.error ="File can't be empty"
			return;
		}
		var serverUrl=BBRoutes.com.baasbox.controllers.Admin.setConfiguration(section,"dummy",s.key, $scope.file.name).absoluteURL();
		if (window.location.protocol == "https:"){
			serverUrl=serverUrl.replace("http:","https:");
		}
		var options = {
				url: serverUrl,
				method:"PUT",
				type: "PUT",
				dataType: "json",
				clearForm: true,
				resetForm: true,
				success: function(){
					alert("File has been uploaded successfully");
					$scope.$apply(function(scope){
						s.filename=$scope.file.name;
						s.changed = false;
					});
				}, //success
				error: function(data) {
					alert("There was an error uploading the file.Please check your logs");
					_this.$apply(function(scope){
						jsonResponse=JSON.parse(data.responseText);
						s.error = jsonResponse["message"];
					});
					
					//console.debug(data);
				}
		};
		$('#'+$scope.keyName(s.key)).ajaxSubmit(options);
	}//updateFileSetting
	
	$scope.set_color = function(k){
		switch(k) {
		case 0:
	        color="#F2F2F2";
	        break;
	    case 1:
	        color="#F2F2F2"
	        break;
	    case 2:
	    	color="#F2F2F2"
	    	break;
	    default:
	    	color="#F2F2F2";
	    	break;
		}
		return { "background-color": color };
	}
	
	$scope.keyName = function(k){
		if (k==null) return "";
		return k.replace(/\./g,'');
	}
	$scope.setFiles = function(element) {
	    $scope.$apply(function(scope) {
	        scope.file =  element.files[0]
	      });
	};//setFiles
	$scope.$watch('data', function () {
		_this.defaultEnabled=_this.isEnabled(1);
		_this.profile3Enabled=_this.isEnabled(2);
		_this.profile3Enabled=_this.isEnabled(3);
	});
	
	$scope.editing=function($event){
		console.log($event);
	}
}


