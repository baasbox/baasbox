/**
 * javascript functions for the Admin GUI 
 */
var userDataArray;
var settingDataArray;
var settingPwdDataArray;
var settingImgDataArray;
var settingSectionChanged;
var settingPushDataArray;
var refreshSessionToken;

$(document).ready(function(){
	setup();
});

function resetChosen(chosenElement){
	//Get the dynamic id given to your select by Chosen
	var selId = $(chosenElement).attr('id');
	//Use that id to remove the dynamically created div (the foe select box Chosen creates)
	$('#'+ selId +'_chzn').remove();
	//Change the value of your select, trigger the change event, remove the chzn-done class, and restart chosen for that select
	$('#'+selId).val('').change().removeClass('chzn-done').chosen();
}

// see http://codeaid.net/javascript/convert-size-in-bytes-to-human-readable-format-(javascript)
function bytesToSize(bytes, precision) {
	if (bytes){
		var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
		
		if (bytes == 0) return 'n/a';
		if (bytes < 1024) {
				return Number(bytes) + " " + sizes[0];
		}
		var posttxt = 0;
		do {
			posttxt++;
			bytes = bytes / 1024;
		} while( bytes >= 1024 );
		bytes=Math.round(bytes * 10) / 10
		return  bytes.toFixed(precision) + " " + sizes[posttxt];

	} else return "n/a";
}

$('.btn-newcollection').click(function(e){
	$('#newCollectionModal').modal('show');
	$("#newCollectionName").val("");
}); // Show Modal for new collection

$('.btn-changepwd').click(function(e){
	$('#changePwdModal').modal('show');
}); // Show Modal for Change Password

$('.btn-adduser').click(function(e){
	loadUserRole();
	resetAddUserForm();
	$("#userTitle").text("Create new User");
	$(".groupUserPwd").removeClass("hide");
	$("#txtUsername").removeClass("disabled");
	$("#txtUsername").prop('disabled', false);
	$('#addUserModal').modal('show');
}); // Show Modal for Add User

$('.btn-newAsset').click(function(e){
	
	$(".error").removeClass("error");
	$('#txtAssetName').val('');
	$('#txtAssetMeta').val('');
	$('#fuAsset').val('');
	$("#errorAsset").addClass("hide");	
	$(".filename").text("no file selected")	
	$('#addAssetModal').modal('show');
}); // Show Modal for New Asset

$(".btn-action").live("click", function() {
	var action = $(this).attr("action");
	var actionType = $(this).attr("actionType");
	var parameters = $(this).attr("parameters");

	
	switch (action)	{
		case "insert":
			switch (actionType)	{
				case "user":
					break;
				case "collection":
					break;
				case "document":
					break;
				case "asset":
					break;
			}
			break;
		case "edit":
			switch (actionType)	{
				case "user":
					openUserEditForm(parameters);
					break;
				case "collection":
					break;
				case "setting":
					openSettingEditForm(parameters);
				case "document":
					break;
				case "asset":
					break;
			}
			break;
		case "delete":
			switch (actionType)	{
				case "user":
					break;
				case "collection":
					break;
				case "document":
					break;
				case "asset":
					if(!confirm("Do you want delete '"+ parameters +"' asset?"))
						return;
					deleteAsset(parameters);
					break;
			}
			break;
	}
});

function deleteAsset(assetName)
{
	BBRoutes.com.baasbox.controllers.Asset.delete(assetName).ajax(
	{
		data: {"name": assetName},
		error: function(data)
		{
			alert(JSON.parse(data.responseText)["message"]);
		},
		success: function(data)
		{
			loadAssetTable();
		}
	})	
}

function openUserEditForm(editUserName)
{
	var userObject;

	resetAddUserForm();
	loadUserRole();
	$("#txtUsername").addClass("disabled");
	$("#txtUsername").prop('disabled', true);
	$("#userTitle").text("Edit User information");
	$(".groupUserPwd").addClass("hide");
	
	for(i=0;i<userDataArray.length;i++)
	{
		if(userDataArray[i].user.name == editUserName)
			userObject = userDataArray[i];
	}

	$("#txtUsername").val(userObject.user.name);
	$("#cmbSelectRole option:contains("+ userObject.user.roles[0].name +")").prop('selected',true)
	$("#cmbSelectRole").trigger("liszt:updated");
	
	$("#txtVisibleByTheUser").val(reverseJSON(userObject.visibleByTheUser));
	$("#txtVisibleByFriend").val(reverseJSON(userObject.visibleByFriend));
	$("#txtVisibleByRegisteredUsers").val(reverseJSON(userObject.visibleByRegisteredUsers));
	$("#txtVisibleByAnonymousUsers").val(reverseJSON(userObject.visibleByAnonymousUsers));

	$('#addUserModal').modal('show');
}

function openSettingEditForm(editSettingName)
{
	var settingObject;
	
	for(i=0;i<settingDataArray.length;i++)
	{
		if(settingDataArray[i].key == editSettingName){
			settingSectionChanged = 'Application';
			settingObject = settingDataArray[i];
		}
	}
	for(i=0;i<settingPwdDataArray.length;i++)
	{
		if(settingPwdDataArray[i].key == editSettingName) {
			settingSectionChanged = 'PasswordRecovery';
			settingObject = settingPwdDataArray[i];
		}
	}
	for(i=0;i<settingImgDataArray.length;i++)
	{
		if(settingImgDataArray[i].key == editSettingName) {
			settingSectionChanged = 'Images';
			settingObject = settingImgDataArray[i];
		}
			
	}
	for(i=0;i<settingPushDataArray.length;i++)
	{
		if(settingPushDataArray[i].key == editSettingName) {
			settingSectionChanged = 'Push';
			settingObject = settingPushDataArray[i];
		}
			
	}	
	$("#lblDescription").text(settingObject.description);
	$("#txtKey").val(settingObject.key);
	$("#txtKey").addClass("disabled");
	$("#txtValue").val(settingObject.value);
	
	$('#EditSettingModal').modal('show');
}

function reverseJSON(objJSON)
{
	var strJSON = JSON.stringify(objJSON);

	if(strJSON == undefined)
		return "";

	var obj=JSON.parse(strJSON); 

	delete obj["_allow"];
	delete obj["_allowRead"];
	return JSON.stringify(obj, undefined, 2);
}

function loadAssetTable()
{
	callMenu("#assets");
}

function loadUserTable()
{
	callMenu("#users");
}

function loadSettingTable()
{
	callMenu("#settings");
}

function loadCollectionsTable()
{
	callMenu("#collections");
}

function resetAddUserForm()
{
	$("#userForm")[0].reset();
	$("#cmbSelectRole option:first").prop('selected',true)
	$("#cmbSelectRole").trigger("liszt:updated");
	$("#errorAddUser").addClass("hide");
	$(".error").removeClass("error");
}

function loadUserRole()
{
		BBRoutes.com.baasbox.controllers.Admin.getRoles().ajax({
			data: {orderBy: "name asc"},
			success: function(data) {
										var sel=$('#cmbSelectRole');
										sel.empty();
										sel.append($("<option/>"));
										data=data["data"];
										$.each(data, function(index, item) {
										    sel.append($("<option/>", {
										        value: item["name"],
										        text: item["name"]
										    }));
										});
										$("#cmbSelectRole").trigger("liszt:updated");
									}});
}

function addUser()
{
	var userName = $("#txtUsername").val();
	var password = $("#txtPassword").val();
	var role = $("#cmbSelectRole").val();
	var visibleByTheUser = ($("#txtVisibleByTheUser").val() == "") ? "{}": $("#txtVisibleByTheUser").val(); 
	var visibleByFriend = ($("#txtVisibleByFriend").val() == "") ? "{}": $("#txtVisibleByFriend").val();
	var visibleByRegisteredUsers = ($("#txtVisibleByRegisteredUsers").val() == "") ? "{}": $("#txtVisibleByRegisteredUsers").val();
	var visibleByAnonymousUsers = ($("#txtVisibleByAnonymousUsers").val() == "") ? "{}": $("#txtVisibleByAnonymousUsers").val();
	
	BBRoutes.com.baasbox.controllers.Admin.createUser().ajax(
	{
		data: JSON.stringify({"username": userName,
								"password": password,
								"role": role,
								"visibleByTheUser": jQuery.parseJSON(visibleByTheUser),
								"visibleByFriend": jQuery.parseJSON(visibleByFriend),
								"visibleByRegisteredUsers": jQuery.parseJSON(visibleByRegisteredUsers),
								"visibleByAnonymousUsers": jQuery.parseJSON(visibleByAnonymousUsers)
							}),
		contentType: "application/json",
		processData: false,
		error: function(data)
		{
			alert(JSON.parse(data.responseText)["message"]);
		},
		success: function(data)
		{
			closeUserForm();
		}
	})	
}

function updateUser()
{
	var userName = $("#txtUsername").val();
	var password = $("#txtPassword").val();
	var role = $("#cmbSelectRole").val();
	var visibleByTheUser = ($("#txtVisibleByTheUser").val() == "") ? "{}": $("#txtVisibleByTheUser").val(); 
	var visibleByFriend = ($("#txtVisibleByFriend").val() == "") ? "{}": $("#txtVisibleByFriend").val();
	var visibleByRegisteredUsers = ($("#txtVisibleByRegisteredUsers").val() == "") ? "{}": $("#txtVisibleByRegisteredUsers").val();
	var visibleByAnonymousUsers = ($("#txtVisibleByAnonymousUsers").val() == "") ? "{}": $("#txtVisibleByAnonymousUsers").val();
	
	BBRoutes.com.baasbox.controllers.Admin.updateUser(userName).ajax(
	{
		data: JSON.stringify({
								"role": role,
								"visibleByTheUser": jQuery.parseJSON(visibleByTheUser),
								"visibleByFriend": jQuery.parseJSON(visibleByFriend),
								"visibleByRegisteredUsers": jQuery.parseJSON(visibleByRegisteredUsers),
								"visibleByAnonymousUsers": jQuery.parseJSON(visibleByAnonymousUsers)
							}),	
		contentType: "application/json",
		processData: false,
		error: function(data)
		{
			alert(JSON.parse(data.responseText)["message"]);
		},
		success: function(data)
		{
			closeUserForm();
		}
	})	
}

function closeUserForm()
{
	$('#addUserModal').modal('hide');
	loadUserTable();
}

function updateSetting()
{
	var key = $("#txtKey").val();
	var value = $("#txtValue").val();
		
	BBRoutes.com.baasbox.controllers.Admin.setConfiguration(settingSectionChanged,"dummy",key, value).ajax(
	{
		
		error: function(data)
		{
			//console.log(data)
			alert("Error updating settings:" + data["message"]);
		},
		success: function(data)
		{
			closeSettingForm();
		}
	})	
}

function closeSettingForm()
{
	$('#EditSettingModal').modal('hide');
	loadSettingTable();
}

$('.btn-UserCommit').click(function(e){
	var action;

	var userName = $("#txtUsername").val();
	var password = $("#txtPassword").val();
	var retypePassword = $("#txtRetypePassword").val();
	var role = $("#cmbSelectRole").val();
	var visibleByTheUser = ($("#txtVisibleByTheUser").val() == "") ? "{}": $("#txtVisibleByTheUser").val(); 
	var visibleByFriend = ($("#txtVisibleByFriend").val() == "") ? "{}": $("#txtVisibleByFriend").val();
	var visibleByRegisteredUsers = ($("#txtVisibleByRegisteredUsers").val() == "") ? "{}": $("#txtVisibleByRegisteredUsers").val();
	var visibleByAnonymousUsers = ($("#txtVisibleByAnonymousUsers").val() == "") ? "{}": $("#txtVisibleByAnonymousUsers").val();
	
	var errorMessage = '';
	
	if($('.groupUserPwd').hasClass('hide'))
		action = "Update";
	else
		action = "Insert";

	if($.trim(userName) == "")
		errorMessage = "The field 'Username' is required<br/>"

	if(action == "Insert"){
		if($.trim(password) == "")
			errorMessage += "The field 'Password' is required<br/>"

		if($.trim(retypePassword) == "")
			errorMessage += "The field 'Retype Password' is required<br/>"
		
		if(password != retypePassword)
		{
			$(".groupUserPwd").addClass("error");
			errorMessage += "'Password' and 'Retype Password' don't match<br/>"
		}
		else
			$(".groupUserPwd").removeClass("error");		
	}

	
	if($.trim(role) == "")
		errorMessage += "The field 'Role' is required<br/>"
	
	if(!isValidJson(visibleByTheUser)){
		$("#auVisibleByTheUser").addClass("error");
		errorMessage += "The field 'Visible By The User' must be a valid Json text<br/>"
	}
	else
		$("#auVisibleByTheUser").removeClass("error");

	if(!isValidJson(visibleByFriend)){
		$("#auVisibleByFriend").addClass("error");
		errorMessage += "The field 'Visible By Friend' must be a valid Json text<br/>"
	}
	else
		$("#auVisibleByFriend").removeClass("error");

	if(!isValidJson(visibleByRegisteredUsers)){
		$("#auVisibleByRegisteredUsers").addClass("error");
		errorMessage += "The field 'Visible By Registered Users' must be a valid Json text<br/>"
	}
	else
		$("#auVisibleByRegisteredUsers").removeClass("error");

	if(!isValidJson(visibleByAnonymousUsers)){
		$("#auVisibleByAnonymousUsers").addClass("error");
		errorMessage += "The field 'Visible By Anonymous Users' must be a valid Json text<br/>"
	}
	else
		$("#auVisibleByAnonymousUsers").removeClass("error");
	
	if(errorMessage != "")
	{
		$("#errorAddUser").html(errorMessage);
		$("#errorAddUser").removeClass("hide");
		return;
	}
	
	if(action == "Insert")
		addUser();
	else
		updateUser();
	
	return;
}); // Validate and Ajax submit for Insert/Update User

$('.btn-SettingCommit').click(function(e){
	var action;

	var key = $("#txtKey").val();
	var value = $("#txtValue").val();
	
	var errorMessage = '';
	
	if($.trim(key) == "")
		errorMessage = "The field 'Key' is required<br/>"

	if($.trim(value) == "")
		errorMessage += "The field 'Value' is required<br/>"
	
	if(errorMessage != "")
	{
		$("#errorEditSetting").html(errorMessage);
		$("#errorEditSetting").removeClass("hide");
		return;
	}
	$("#txtKey").removeClass("disabled");
	updateSetting();
	
	return;
}); // Validate and Ajax submit for Update Setting


function isValidJson(code)
{
    try {
        JSON.parse(code);
		return true;
    } catch (e) {
        return false;
    }
}

$('.btn-NewCollectionCommit').click(function(e){

	var collectionName = $("#newCollectionName").val();
	
	if($.trim(collectionName) == '')
	{
		return;
	}		
	
	BBRoutes.com.baasbox.controllers.Admin.createCollection(collectionName).ajax(
	{
		data: JSON.stringify({"name": collectionName}),
		contentType: "application/json",
		processData: false,
		error: function(data)
		{
			alert(JSON.parse(data.responseText)["message"]);
		},
		success: function(data)
		{
			$('#newCollectionModal').modal('hide');
			loadCollectionsTable();
		}
	})	
}); // Validate and Ajax submit for new collection

$('.btn-ChangePwdCommit').click(function(e){

	var oldPassword = $("#oldpassword").val();
	var newPassword = $("#newpassword").val();

	if(sessionStorage.password != oldPassword)
	{
		$("#errorCPwd").removeClass("hide");
		return;
	}
	else
		$("#errorCPwd").addClass("hide");
	
	if(newPassword != $("#retypenewpassword").val())
	{
		$("#errorPwdNotMatch").removeClass("hide");
		return;
	}		
	else
		$("#errorPwdNotMatch").addClass("hide");
		
	if($.trim(newPassword) == '')
	{
		$("#errorNewPassword").removeClass("hide");
		return;
	}		
	else
		$("#errorNewPassword").addClass("hide");
	
	BBRoutes.com.baasbox.controllers.User.changePassword().ajax(
	{
		data: JSON.stringify({"old": sessionStorage.password,"new": newPassword}),
		contentType: "application/json",
		processData: false,
		error: function(data)
		{
			alert(JSON.parse(data.responseText)["message"]);
		},
		success: function(data)
		{
			sessionStorage.password = newPassword;
			$('#changePwdModal').modal('hide');
		}
	})	
}); // Validate and Ajax submit for Change Password

$('#assetForm').submit(function() {

	var assetName = $("#txtAssetName").val();
	var assetMeta = ($("#txtAssetMeta").val() == "") ? "{}": $("#txtAssetMeta").val();

	var errorMessage = '';

	if($.trim(assetName) == "")
		errorMessage = "The field 'Name' is required<br/>"

//	if($.trim(assetMeta) == "")
//		errorMessage += "The field 'Meta' is required<br/>"
		
	if(!isValidJson(assetMeta)){
		$("#divAssetMeta").addClass("error");
		errorMessage += "The field 'Meta' must be a valid Json text<br/>"
	}
	else
		$("#divAssetMeta").removeClass("error");

	var ext = $('#fuAsset').val().split('.').pop().toLowerCase();
	/*if($.inArray(ext, ['gif','png','jpg','jpeg','tif','eps','pdf','doc','docx','xls','xlsx','ppt','pptx','txt','']) == -1) {
		errorMessage += "Invalid File Extension<br/>"
	}*/		
		
	if(errorMessage != "")
	{
		$("#errorAsset").html(errorMessage);
		$("#errorAsset").removeClass("hide");
		return;
	}

	if ($.trim(assetMeta) == "") 
		assetMeta = "{}";
	
	var serverUrl=BBRoutes.com.baasbox.controllers.Asset.post().absoluteURL();
	if (window.location.protocol == "https:"){
		serverUrl=serverUrl.replace("http:","https:");
	}

	var options = {
		url: serverUrl,
		type: "post",
		dataType: "json",
		clearForm: true,
		resetForm: true,
			success: function(){
				$('#addAssetModal').modal('hide');
				loadAssetTable();
			} //success
		};

 	$(this).ajaxSubmit(options);

	return false;
})// Validate and Ajax submit for new Asset


function make_base_auth(user, password) {
	  var tok = user + ':' + password;
	  var hash = btoa(tok);
	  return "Basic " + hash;
	}

function setup(){
    
	setupAjax();
	setupMenu();
	setupTables();
	setupSelects();

	$('.logout').click(function(e){
		BBRoutes.com.baasbox.controllers.User.logoutWithoutDevice().ajax({}).always(
				function() { 
					sessionStorage.up="";
					sessionStorage.appcode="";
					sessionStorage.sessionToken="";
					location.reload(); 
				});
	});
	
	if (sessionStorage.up && sessionStorage.up!="") {
		tryToLogin();
	}	
}

function getActionButton(action, actionType,parameters){

	var iconType;
	var classType;
	var labelName;
	
	switch (action)	{
		case "edit":
			iconType = "icon-edit";
			classType = "btn-info";
			labelName = "Edit";
			break;
		case "delete":
			iconType = "icon-trash";
			classType = "btn-danger";
			labelName = "Delete";
			break;
	}
	var actionButton = "<a class='btn "+ classType +" btn-action' action='"+ action +"' actionType='"+ actionType +"' parameters='"+ parameters +"' href='#'><i class='"+ iconType +"' icon-white'></i> "+ labelName +"</a>";
	return actionButton;
}


function setBradCrumb(type)
{
	var sBradCrumb;
	
	switch (type){
		case "#users":
			sBradCrumb = "Users";
		  break;
		case "#dashboard":
			sBradCrumb = "Dashboard";
		  break;
		case "#settings":
			sBradCrumb = "Settings";
		  break;
		case "#collections":
			sBradCrumb = "Collections";
		  break;
		case "#documents":
			sBradCrumb = "Documents";
		  break;
		case "#assets":
			sBradCrumb = "Assests";
		  break;
	}

	$("#bradcrumbItem").text(sBradCrumb);
}

function getAssetIcon(type)
{
	var sIcon="";
	
	switch (type){
		case "image/png":
			sIcon = "png.png";
		  break;
		case "image/jpeg":
			sIcon = "jpg.png";
		  break;
		case "image/gif":
			sIcon = "gif.png";
		  break;
		case "image/tiff":
			sIcon = "tiff.png";
		  break;
		case "image/eps":
			sIcon = "eps.png";
		  break;
		case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
			sIcon = "word.png";
		  break;
		case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
			sIcon = "excel.png";
		  break;
		case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
			sIcon = "ppt.png";
		  break;
		case "application/pdf":
			sIcon = "pdf.png";
		  break;
	}
	if (sIcon=="") return "";
	return "<img src='img/AssetIcons/"+ sIcon +"'/>";
}


function setupTables(){
    $('#userTable').dataTable( {
    	"sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
		"sPaginationType": "bootstrap",
		"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
    	"aoColumns": [ {"mData": "user.name"},
    	               {"mData": "user.roles.0.name"},
    	               {"mData": "_creation_date","sDefaultContent":""},
    	               {"mData": "user.status","mRender": function ( data, type, full ) {
    	            	   										var classStyle="label-success"
    	            	   										if (data!="ACTIVE") classStyle="label-important";
    	            	   										var htmlReturn="<span class='label "+classStyle+"'>"+data+"</span> ";
    	            	   										return htmlReturn;
    	               								}
    					},
					   {"mData": "user.name", "mRender": function ( data, type, full ) {
														if(data!="admin" && data!="baasbox")
															return getActionButton("edit","user",data);// +" "+ getActionButton("delete","user",data);
														return "";
    	               								}
    	               }],
        "bRetrieve": true,
  		"bDestroy":true
        } ).makeEditable();

    $('#settingsTable').dataTable( {
    	"sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
		"sPaginationType": "bootstrap",
		"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
    	"aoColumns": [ {"mData": "key"},
    	               {"mData": "description"},
    	               {"mData": "value", "mRender":function ( data, type, full ) {
															return $('<div/>').text(data).html();
					   								}
					   },
					   
					   {"mData": "key", "mRender": function ( data, type, full ) {
															return getActionButton("edit","setting",data);
					   										}
    	               }],
    	               
        "bRetrieve": true,
  		"bDestroy":false
        } ).makeEditable();

    $('#settingsPwdTable').dataTable( {
    	"sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
		"sPaginationType": "bootstrap",
		"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
    	"aoColumns": [ {"mData": "key"},
    	               {"mData": "description"},
    	               {"mData": "value", "mRender":function ( data, type, full ) {
															return $('<div/>').text(data).html();
					   								}
					   },
					   {"mData": "key", "mRender": function ( data, type, full ) {
															return getActionButton("edit","setting",data);
					   										}
    	               }],
    	               
        "bRetrieve": true,
  		"bDestroy":false
        } ).makeEditable();
    
    $('#settingsImgTable').dataTable( {
    	"sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
		"sPaginationType": "bootstrap",
		"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
    	"aoColumns": [ {"mData": "key"},
    	               {"mData": "description"},
    	               {"mData": "value", "mRender":function ( data, type, full ) {
															return $('<div/>').text(data).html();
					   								}
					   },
					   {"mData": "key", "mRender": function ( data, type, full ) {
															return getActionButton("edit","setting",data);
					   										}
    	               }],
    	               
        "bRetrieve": true,
  		"bDestroy":false
        } ).makeEditable();

    $('#settingsPushTable').dataTable( {
    	"sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
		"sPaginationType": "bootstrap",
		"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
    	"aoColumns": [ {"mData": "key"},
    	               {"mData": "description"},
    	               {"mData": "value", "mRender":function ( data, type, full ) {
															return $('<div/>').text(data).html();
					   								}
					   },
					   {"mData": "key", "mRender": function ( data, type, full ) {
															return getActionButton("edit","setting",data);
					   										}
    	               }],
    	               
        "bRetrieve": true,
  		"bDestroy":false
        } ).makeEditable();

    
    $('#collectionTable').dataTable( {
    	"sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
		"sPaginationType": "bootstrap",
		"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
    	"aoColumns": [ {"mData": "name"}],
        "bRetrieve": true,
  		"bDestroy":true
        } ).makeEditable();
    $('#documentTable').dataTable( {
    	"sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
		"sPaginationType": "bootstrap",
		"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
    	"aoColumns": [ {"mData": "@rid"},
    	               {"mData": "@rid", "mRender": function ( data, type, full ) {
    	            	   								var obj=JSON.parse(JSON.stringify(full)); 
    	            	   								delete obj["@rid"];
    	            	   								delete obj["@class"];
    	            	   								delete obj["@version"];
    	            	   								return "<pre>" + JSON.stringify(obj, undefined, 2) + "</pre>";
    	               								}
    	               },
    	               {"mData": "@version"}
    	               ],
        "bRetrieve": true,
  		"bDestroy":true
        } ).makeEditable(); 
	$('#btnReloadDocumkents').click(function(){
			$("#selectCollection").trigger("change");
		});
    $('#assetTable').dataTable( {
    	"sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
		"sPaginationType": "bootstrap",
		"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
    	"aoColumns": [ 
						{"mData": "@class", "mRender": function (data, type, full ) {
														var obj=JSON.parse(JSON.stringify(full));
														if(data =="_BB_FileAsset")
															return getAssetIcon(obj["contentType"]);
    	            	   								return "";
    	               								}},
						{"mData": "name"},
						{"mData": "@class", "mRender": function ( data, type, full ) {
														var obj=JSON.parse(JSON.stringify(full));
														if(obj["meta"] != undefined)
														{
															return JSON.stringify(obj["meta"]);
														}
														else
														{
															return "";
														}
    	               								}},
						{"mData": "@class", "mRender": function (data, type, full ) {
														var obj=JSON.parse(JSON.stringify(full));
														if(data =="_BB_FileAsset")
															return  bytesToSize(obj["contentLength"],'KB');
    	            	   								return "";
    	               								}},
						{"mData": "@class", "mRender": function (data, type, full ) {
														var obj=JSON.parse(JSON.stringify(full));
														if(data =="_BB_FileAsset")
															return obj["contentType"];
    	            	   								return "";
    	               								}},
						{"mData": "@class", "mRender": function (data, type, full) {
														var obj=JSON.parse(JSON.stringify(full));
														if(data =="_BB_FileAsset")
															return "<a href='/asset/" + obj["name"] + "/download?X-BAASBOX-APPCODE="+ escape($("#login").scope().appcode) +"' target='_new'>"+ obj["fileName"] +"</a>";
    	            	   								return "";
    	               								}},
						{"mData": "name", "mRender": function (data) {
    	            	   								return getActionButton("delete","asset",data);
    	               								}}
		],
        "bRetrieve": true,
  		"bDestroy":true
        } ).makeEditable();
}

function setupSelects(){

	$("#selectCollection").chosen().change(function(){
			val=$("#selectCollection").val();
			BBRoutes.com.baasbox.controllers.Document.getDocuments(val).ajax({
				data: {orderBy: "name asc"},
				success: function(data) {
											data=data["data"];
											var scope=$("#documents").scope();
											scope.$apply(function(){
										    	 scope.collectioName=val;
										     });	
											$('#documentTable').dataTable().fnClearTable();
											$('#documentTable').dataTable().fnAddData(data);
										}
			})//BBRoutes.com.baasbox.controllers.Document.getDocuments
	});//selectCollection
}

function setupAjax(){
	$.ajaxSetup({
			beforeSend: function (xhr){ 
				xhr.setRequestHeader('X-BB-SESSION', sessionStorage.sessionToken);
			},
			statusCode: {
				401: function(){
						alert("Sorry, session expired. You must login again");
						location.reload();
					}
				}
			}
	);
	//hack for charisma menu
	$('#for-is-ajax').hide();
	$('#is-ajax').prop('checked',true);
}//setupAjax

function setupMenu(){
	//ajaxify menus
	$('a.ajax-link').click(function(e){
		//console.log("a.ajax-link click");
		//console.log(e);
		if($.browser.msie) e.which=1;
		//console.log("go on...");
		e.preventDefault();
		var $clink=$(this);
		callMenu($clink.attr('href'));
		$('ul.main-menu li.active').removeClass('active');
		$clink.parent('li').addClass('active');
	});
}//setupMenu

function applySuccessMenu(action,data){
	$('#loading').remove();
	var scope=$("#loggedIn").scope();
     scope.$apply(function(){
    	 scope.menu=action.substr(1);
     });
     var scope=$(action).scope();
     scope.$apply(function(){
    	 scope.data=data;
     });	

}//applySuccessMenu 


function callMenu(action){
	var scope=$("#loggedIn").scope();
    scope.$apply(function(){
   	 scope.menu="";
    });
	$('#loading').remove();
	$('#content').parent().append('<div id="loading" class="center">Loading...<div class="center"></div></div>');

	setBradCrumb(action);

	switch (action)	{
	case "#users":
		BBRoutes.com.baasbox.controllers.Admin.getUsers().ajax({
			data: {orderBy: "user.name asc"},
			success: function(data) {
										userDataArray = data["data"];
										console.log("Admin.getUsers success:");
										console.log(data);
										applySuccessMenu(action,userDataArray);
										$('#userTable').dataTable().fnClearTable();
										$('#userTable').dataTable().fnAddData(userDataArray);
									}
		});
	  break;//#users
	case "#dashboard":
		BBRoutes.com.baasbox.controllers.Admin.getLatestVersion().ajax({
			success: function(data)	{
				var data = data["data"];
				$('#latestVersion').text(data["latest_version"]);
				var url=data["announcement_url"];
				if (url=="") url = data["download_url"];
				$('#getLatestVersion').prop("href", url);
				if ($('#currentVersion').text()<data["latest_version"]+"") {
					$('#notificationLatestVersion').text("!");
					$('#notificationLatestVersion').addClass("notification red");
				}
			}, //success
			error: function(data){
				$('#latestVersion').addClass("red").text("Unable to contact the BaasBox site");
				$('#notificationLatestVersion').text("?");
				$('#notificationLatestVersion').addClass("notification yellow");
			}
		});//BBRoutes.com.baasbox.controllers.Admin.getLatestVersion().ajax
		BBRoutes.com.baasbox.controllers.Admin.getDBStatistics().ajax({
			success: function(data) {
							var data = data["data"];
							applySuccessMenu(action,data);
									var platformSpecificFeed={
											"bbid": data["installation"]["bb_id"],
											"bbv":  data["installation"]["bb_version"],
											"odbv": data["db"]["properties"]["version"],
											"osn": data["os"]["os_name"],
											"osa": data["os"]["os_arch"],
											"osv": data["os"]["os_version"],
											"c": data["os"]["processors"],
											"jvmmm": data["memory"]["max_allocable_memory"],
											"jvv": data["java"]["java_vendor"],
											"jve": data["java"]["java_version"],
											"rand": Math.random().toString(36).substr(2,7)
									};
									$('#latestNewsTab').rssfeed('http://www.baasbox.com/feed/', {
												header: false,
												dateformat: "date",
												content:true,
												snippet:true,
												linktarget: "_blank",
												linkcontent: true,
												limit: 5,
												errormsg: "Unable to retrieve latest news"
									});
									var serverPlatform=getPlatform(data["os"]["os_name"]);
									$('#platformNameNews').text(data["os"]["os_name"]);
									$('#platformNewsTab').rssfeed('http://www.baasbox.com/tag/'+serverPlatform+'/feed/?' + $.param(platformSpecificFeed,true), 
										{
													header: false,
													dateformat: "date",
													content:true,
													snippet:true,
													linktarget: "_blank",
													linkcontent: true,
													limit: 5,
													errormsg: "Unable to retrieve latest news about BaasBox on " + data["os"]["os_name"] + " platform"
										});
									
					}//success function
		});//ajax call

	  break;	//#dashboard
	case "#settings":		
		BBRoutes.com.baasbox.controllers.Admin.getConfiguration("Application").ajax({
			success: function(data) {
				console.log("dumpConfiguration Application success:");
				console.log(data);
				settingDataArray = data["data"];
				
				//applySuccessMenu(action,data);
				$('#settingsTable').dataTable().fnClearTable();
				$('#settingsTable').dataTable().fnAddData(settingDataArray);
			}
		});
		BBRoutes.com.baasbox.controllers.Admin.getConfiguration("PasswordRecovery").ajax({
			success: function(data) {
				console.log("dumpConfiguration PasswordRecovery success:");
				console.log(data);
				settingPwdDataArray = data["data"];
				//applySuccessMenu(action,data);
				$('#settingsPwdTable').dataTable().fnClearTable();
				$('#settingsPwdTable').dataTable().fnAddData(settingPwdDataArray);
			}
		});
		BBRoutes.com.baasbox.controllers.Admin.getConfiguration("Push").ajax({
			success: function(data) {
				console.log("dumpConfiguration Push success:");
				console.log(data);
				settingPushDataArray = data["data"];
				//applySuccessMenu(action,data);
				$('#settingsPushTable').dataTable().fnClearTable();
				$('#settingsPushTable').dataTable().fnAddData(settingPushDataArray);
			}
		});
		BBRoutes.com.baasbox.controllers.Admin.getConfiguration("Images").ajax({
			success: function(data) {
				console.log("dumpConfiguration Images success:");
				console.log(data);
				settingImgDataArray = data["data"];
				console.log("action: ");
				console.log(action);
				applySuccessMenu(action,settingImgDataArray);
				$('#settingsImgTable').dataTable().fnClearTable();
				$('#settingsImgTable').dataTable().fnAddData(settingImgDataArray);
			}
		});
	  break;	//#settings
	  
	case "#collections":
		BBRoutes.com.baasbox.controllers.Admin.getCollections().ajax({
		data: {orderBy: "name asc"},
		success: function(data) {
									applySuccessMenu(action,data["data"]);
									$('#collectionTable').dataTable().fnClearTable();
									$('#collectionTable').dataTable().fnAddData(data["data"]);
								}
		});
	  break;//#collections
	case "#documents":
		$('#documentTable').dataTable().fnClearTable();
		BBRoutes.com.baasbox.controllers.Admin.getCollections().ajax({
			data: {orderBy: "name asc"},
			success: function(data) {
										data=data["data"];
										applySuccessMenu(action,data);
										var sel=$('#selectCollection');
									    sel.empty();

										resetChosen("#selectCollection");

										sel.append($("<option/>"));
										$.each(data, function(index, item) {
										    sel.append($("<option/>", {
										        value: item["name"],
										        text: item["name"]
										    }));
										});
										sel.trigger("liszt:updated");
									}
		});
	  break; //#documents
	case "#assets":
		BBRoutes.com.baasbox.controllers.Asset.getAll().ajax({
			data: {orderBy: "name asc"},
			success: function(data) {
										data=data["data"];
										applySuccessMenu(action,data);
										//console.log("getAll");
										//console.log(data);
										$('#assetTable').dataTable().fnClearTable();
										$('#assetTable').dataTable().fnAddData(data);
									}
		});
	  break;//#assets	  
	}
}//callMenu
function AssetsController($scope){
}
function UsersController($scope){
}
function CollectionsController($scope){
}
function DocumentsController($scope){
}

  function tryToLogin(user, pass,appCode){
		BBRoutes.com.baasbox.controllers.User.login().ajax({
			data:{username:user,password:pass,appcode:appCode},
			success: function(data) {
				         sessionStorage.sessionToken=data["data"]["X-BB-SESSION"];
				         console.log("login success");
				         console.log("data received: ");
				         console.log(data);
				         console.log("sessionStorage.sessionToken: " + sessionStorage.sessionToken);
						 callMenu("#dashboard");
						 //refresh the sessiontoken every 5 minutes
						 refreshSessionToken=setInterval(BBRoutes.com.baasbox.controllers.Generic.refreshSessionToken().ajax(),300000);
						 var scope=$("#loggedIn").scope();
						 scope.$apply(function(){
						   	 scope.loggedIn=true;
						 });
				     },
			error: function() {
				$("#errorLogin").removeClass("hide");
			}
		});	 //ajax
  }//tryToLogin	

function LoginController($scope) {
	  $scope.login = function() {
			var username=$scope.username;
			var password=$scope.password;
			var appCode=$scope.appcode;
			tryToLogin(username, password,appCode);
	  }; //login
}	//LoginController

function SettingsController($scope){
}

function DashboardController($scope) {
	
	$scope.countDocuments = function(statistics){
		var tot=0;
			//console.log(statistics);
			if (statistics){
			angular.forEach(statistics.data.collections_details, function(value){
			  tot += value.records;
			});
		}
		return tot;
	}
	
	$scope.formatSize = function(size){
		return bytesToSize(size,2);
	}
	
}


	
	function getPlatform(os){
		function isWindows(OS) {
			return (OS.indexOf("win") >= 0);
		}
	 
		function isMac(OS) {
			return (OS.indexOf("mac") >= 0);
		}
	 
		function isUnix(OS) {
			return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
		}
	 
		function  isSolaris(OS) {
			return (OS.indexOf("sunos") >= 0);
		}
		
     	os = os.toLowerCase();
		if (isWindows(os)) {
			return "windows";
		} else if (isMac(os)) {
			return "mac";
		} else if (isUnix(os)) {
			return "unix,linux";
		} else if (isSolaris(os)) {
			return "solaris,unix";
		} else {
			return "other";
		}
	}