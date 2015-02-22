function openLog(){
	var url = window.location.origin + BBRoutes.com.baasbox.controllers.EventsController.openSystemLogger().url;
	var qs="X-BB-SESSION="+sessionStorage.sessionToken;
	window.open(url + "?" + qs,"BaasBoxLogWindow");
}