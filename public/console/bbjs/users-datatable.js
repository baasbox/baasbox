/**
* Definition of the documents datatable
**/

var  userDataArray= new Array();

   function loadUsersData(){
	    url=BBRoutes.com.baasbox.controllers.Admin.getUsers().absoluteURL();
	    loadTable($('#userTable'),usersDataTableDef,url,userDataArray); //defined in datatable.js
    }
   
   var usersDataTableDef={
			"sDom": "R<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
			"sPaginationType": "bootstrap",
			"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
			"aoColumns": [ {"mData": "user.name", "mRender": function(data,type,full){
					            var html = data;
					            if(data !="admin" && data != "baasbox" && data!="internal_admin"){
					               html = getActionButton("followers","user",data)+"&nbsp;"+html;
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
	                               return getActionButton("edit", "user", data.name) + "&nbsp;" + getActionButton("changePwdUser", "user", data.name) +
	                                   "&nbsp;" + getActionButton(_active?"suspend":"activate", "user", data.name);
	                           }
			            	   return "No action available";
			               },bSortable:false
			               }],
           "bRetrieve": true,
           "bDestroy":true
		}