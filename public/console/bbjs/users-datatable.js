/**
* Definition of the documents datatable
**/

var  userDataArray= new Array();

   function loadUsersData(){
	    url = window.location.origin + BBRoutes.com.baasbox.controllers.Admin.getUsers().url;
	    loadTable($('#userTable'),usersDataTableDef,url,userDataArray); //defined in datatable.js
    }
   
   var usersDataTableDef={
			"sDom": sDomGlobal,
			"sPaginationType": "bootstrap",
			"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
			"aoColumns": [ {"mData": "user.name", "mRender": function(data,type,full){
					            html = data;
					            if(data !="admin" && data != "baasbox" && data!="internal_admin"){
					               html = full.user.name + (full.id?" <br> " + full.id:"");
					               html = getActionButton("followers","user",escape(data))+"&nbsp;"+html;
					            }
					            return html;
							}},
			               {"mData": "user.roles.0.name"},
			               {"mData": "signUpDate","sDefaultContent":""},
			               {"mData": "user.status","mRender": function ( data, type, full ) {
			            	   var classStyle="label-success"
			            		   if (data!="ACTIVE") classStyle="label-important";
			            	   var htmlReturn="<span class='label "+classStyle+"'>"+data+"</span> ";
			            	   return htmlReturn;
			               }
			               },
			               {"mData": "user", "mRender": function ( data, type, full ) {
			            	   if(data.name!="admin" && data.name!="baasbox" && data.name!="internal_admin") {
	                               var _active = data.status == "ACTIVE";
	                               var escapedName=escape(data.name);
	                               return getActionButton("edit", "user", escapedName) + "&nbsp;" + getActionButton("changePwdUser", "user", escapedName) +
	                                   "&nbsp;" + getActionButton(_active?"suspend":"activate", "user", escapedName) +   getActionButton("drop", "user", escapedName);
	                           }
			            	   return "No action available";
			               },bSortable:false
			               }],
           "bRetrieve": true,
           "bDestroy":true
		}