/**
* Definition of the documents datatable
**/

var documentDataArray= new Array();

   function loadDocumentsData(collectionName){
	   if (collectionName!=null){
	    	url=BBRoutes.com.baasbox.controllers.Document.getDocuments(collectionName).absoluteURL();
	    	loadTable($('#documentTable'),documentsDataTableDef,url,documentDataArray); //defined in datatable.js
   		}
    }
   var documentsDataTableDef={
			"sDom": "R<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
			"sPaginationType": "bootstrap",
			"oLanguage": {"sLengthMenu": "_MENU_ records per page"},
			"aoColumns": [{"mData": "_creation_date",sWidth:"85px","mRender": function ( data, type, full ) {
	                        var datetime = data.split("T");
	    	    			return "<span style='font-family:Courier'>"+datetime[0]+"<br/>"+datetime[1]+"</span>";
							}
						   },
						   {"mData": "id", sWidth:"78px","mRender": function ( data, type, full ) 	{
				 				return "<span style='font-family:Courier'>"+data+"</span>";
							}
						   },
			               {"mData": "_author"},
			               {"mData": "@rid",sWidth:"50%","mRender": function ( data, type, full ) 	{
			            	   var obj=JSON.parse(JSON.stringify(full));
			            	   delete obj["@rid"];
			            	   delete obj["@class"];
			            	   delete obj["@version"];
							   delete obj["id"];
							   delete obj["_author"];
							   delete obj["_creation_date"];
			            	   return "<pre>" + JSON.stringify(obj, undefined, 2) + "</pre>";
							},bSortable:false
			               },
			               {"mData": "id","mRender": function ( data, type, full ) {
							   var obj=JSON.parse(JSON.stringify(full));
			            	   return getActionButton("edit","document",data + obj["@class"]) + "&nbsp;" + getActionButton("delete","document",data+obj["@class"]) /*+ "&nbsp;" + getActionButton("acl","document",data+obj["@class"])*/;
			            	},bSortable:false,bSearchable:false
			               }
			               ],
           "bRetrieve": true,
           "bDestroy":true
		}