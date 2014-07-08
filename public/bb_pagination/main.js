/*
 * Auto-generated content from the Brackets New Project extension.  Enjoy!
 */

$(document).ready(function() {
    $.ajaxSetup({
        beforeSend: function (xhr){ 
            xhr.setRequestHeader('Authorization', "BASIC " + btoa("admin" + ":" + "admin")),
            xhr.setRequestHeader('x-baasbox-appcode', '1234567890')
        }
    });
    
    //transforms datatable parameters into BaasBox query criteria, calls the API and converts the result into the datatable format
    function serverDataTableCallback( sSource, aoData, fnCallback ) {
    	//sSource: URL to call
    	//aoData: parameters to use within the call (they must be converted into BaasBox specific parameters)
    	console.log("sSource");
    	console.log(sSource);
        console.log("aoData");
    	console.log(JSON.stringify(aoData));
    	
    	//transforms datatable parameters into BaasBox query criteria
    	var call_id = 0;
    	var iDisplayStart=0;
    	var recordsPerPage = 0;
    	var search=undefined;
    	var sortCol=undefined;
    	var sortDir="";
    	var orderBy=undefined;
    	aoDataLength=aoData.length;
    	for (index = 0; index < aoDataLength; ++index) {
    	    if (aoData[index]["name"]=="sEcho") call_id=aoData[index]["value"];
			if (aoData[index]["name"]=="iDisplayStart") iDisplayStart=aoData[index]["value"] ;
			if (aoData[index]["name"]=="iDisplayLength") recordsPerPage=aoData[index]["value"];
			if (aoData[index]["name"]=="sSearch" && aoData[index]["value"]!="") search="any() like '%" + aoData[index]["value"] +"%'";
			if (aoData[index]["name"]=="iSortCol_0") { //sorting
				var sortColNumber=aoData[index]["value"];
				for (j = 0; j < aoDataLength; ++j) {
					if (aoData[j]["name"]=="mDataProp_" + sortColNumber) sortCol=aoData[j]["value"]; //sorted field name
					if (aoData[j]["name"]=="sSortDir_0") sortDir=aoData[j]["value"] //sort direction
				}
				if (sortCol) orderBy=sortCol + " " + sortDir;
			}	
    	}
    	queryParams = {"call_id":call_id,"skip":iDisplayStart,"page":0,"recordsPerPage":recordsPerPage,"where":search,"orderBy":orderBy};
		//---------------
    	
    	//Calls the BaasBox API
    	//actually performs three calls
		$.getJSON( sSource, queryParams, function (json) { 
			var response={
					"sEcho":json["call_id"],
					"aaData":json["data"]
			}
			//same call but to know the total number of records that match the query
			queryParams.count=true;
			delete queryParams.orderBy;
			$.getJSON( sSource, queryParams, function (json) { 
				response.iTotalDisplayRecords=json["data"][0]["count"];
				//call BaasBox to know the total number or records belonging to the collection
				delete queryParams.where;
				$.getJSON( sSource, queryParams, function (json) { 
					response.iTotalRecords=json["data"][0]["count"];
					console.log(response);
					//returns converted data to datatable
					fnCallback(response);
				}); //getJSON
			}); //getJSON
		}); //getJSON
    }//serverDataTableCallback
    
    /***
     * initialize and load a datatable
     * Example:
     * 	loadTable($('#documentTable'),documentsDataTableDef,url);
     */
    function loadTable(oDataTable,oTableDef,sUrl){
    	if (oDataTable.dataTable()){
    		oDataTable.dataTable().fnDestroy();
    	}
    	var tDef = $.extend({},oTableDef);
    	if (window.location.protocol == "https:"){
    		sUrl=sUrl.replace("http:","https:");
    	}
    	tDef.sAjaxSource= sUrl;
    	tDef.bProcessing = true,
    	tDef.bServerSide = true,
    	tDef.fnServerData= function ( sSource, aoData, fnCallback ) {serverDataTableCallback( sSource, aoData, fnCallback )},
    	oDataTable.dataTable(tDef);
    }//loadTable
    
    //load the documents table with data belonging to the speficied collection
    function loadCollectionData(collectionName){
    	url=BBRoutes.com.baasbox.controllers.Document.getDocuments(collectionName).absoluteURL();
    	loadTable($('#documentTable'),documentsDataTableDef,url);
    	/*
    	$('#documentTable').dataTable( {
    	//http://legacy.datatables.net/examples/server_side/custom_vars.html
    	"bProcessing": true,
        "bServerSide": true,
        "sAjaxSource": BBRoutes.com.baasbox.controllers.Document.getDocuments("test").absoluteURL(),
        "fnServerData": function ( sSource, aoData, fnCallback ) {serverDataTableCallback( sSource, aoData, fnCallback )},
    	});*/
    }
   
    //definition of the documents table 
    var documentsDataTableDef={
            "sDom": "R<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
            "sPaginationType": "full_numbers",
            "oLanguage": {"sLengthMenu": "_MENU_ records per page"},
            "aoColumns": [{"mData": "_creation_date",sWidth:"85px","mRender": function ( data, type, full ) 	{
                                //    	    			console.log("DATA: "+data);
                                var datetime = data.split("T");
                                return "<span style='font-family:Courier'>"+datetime[0]+"<br/>"+datetime[1]+"</span>";
                            }
                          },
                          {"mData": "id", sWidth:"280px","mRender": function ( data, type, full ) 	{
                              return "<span style='font-family:Courier'>"+data+"</span>";
                          }
                          },
                          {"mData": "_author"},
                          {"mData": "@rid","mRender": function ( data, type, full ) 	{
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
                              return 1;
                              //return getActionButton("edit","document",data + obj["@class"]) + "&nbsp;" + getActionButton("delete","document",data+obj["@class"]);
                          },bSortable:false,bSearchable:false
                          }
                         ],
            //"bRetrieve": true,
            "bDestroy":true
        };
    
    $('#documentTable').dataTable();
    loadCollectionData("null");     
    //loadCollectionData("tost");    
} );//ready()

