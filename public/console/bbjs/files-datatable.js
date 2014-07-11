/**
* Definition of the documents datatable
**/

var  filesDataArray= new Array();

   function loadFilesData(){
	    url=BBRoutes.com.baasbox.controllers.File.getAllFile().absoluteURL();
	    loadTable($('#fileTable'),filesDataTableDef,url,filesDataArray); //defined in datatable.js
    }
   
   var filesDataTableDef={
			"sDom": "R<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
			"sPaginationType": "bootstrap",
			"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
			"aoColumns": [
			              {"mData": "id", sWidth:"42px","mRender": function (data, type, full ) {
			            	  var obj=JSON.parse(JSON.stringify(full));
			            	  return getFileIcon(obj["contentType"],obj["id"]);
			              }},
						   {"mData": "id", sWidth:"180px","mRender": function ( data, type, full ) 	{
				 				return "<span style='font-family:Courier'>"+data+"</span>";
							}
						   },
			              {"mData": "id", "mRender": function ( data, type, full ) {
			            	  var obj=JSON.parse(JSON.stringify(full));
			            	  if(obj["attachedData"] != undefined)
			            	  {
			            		  return "<pre>" + JSON.stringify(obj["attachedData"],undefined,2) + "</pre>";
			            	  }
			            	  else
			            	  {
			            		  return "";
			            	  }
			              },bSortable:false},
			              {"mData": "id", "mRender": function (data, type, full ) {
			            	  var obj=JSON.parse(JSON.stringify(full));
		            		  return  bytesToSize(obj["contentLength"],'KB');
			              }},
			              {"mData": "id", sWidth:"210px","mRender": function (data, type, full) {
			            	  var obj=JSON.parse(JSON.stringify(full));
		            		  return "<a href='/file/" + obj["id"] + "?download=true&X-BB-SESSION="+escape(sessionStorage.sessionToken)+"&X-BAASBOX-APPCODE="+ escape($("#login").scope().appcode) +"' target='_new'>"+ obj["fileName"] +"</a>";
			              }},
			              {"mData": "id", "mRender": function (data) {
			            	  return getActionButton("delete","file",data);
			              }}
			              ],
              "bRetrieve": true,
              "bDestroy":true
		}