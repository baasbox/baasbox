/*
Javascript helper functions for server side pagination of datatables
*/

    //transforms datatable parameters into BaasBox query criteria, calls the API and converts the result into the datatable format
    function serverDataTableCallback( sSource, aoData, fnCallback,dataArray ) {
    	//sSource: URL to call
    	//aoData: parameters to use within the call (they must be converted into BaasBox specific parameters)
    	
    	
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
			//load the readed records into the external array
			dataArray.length = 0;
			dataArray.push.apply(dataArray, json["data"]);
			
			//same call but to know the total number of records that match the query
			queryParams.count=true;
			delete queryParams.orderBy;
			delete queryParams.page;
			delete queryParams.recordsPerPage;
			delete queryParams.skip;
			
			$.getJSON( sSource, queryParams, function (json) { 
				response.iTotalDisplayRecords=json["data"][0]["count"];
				//call BaasBox to know the total number or records belonging to the collection
				delete queryParams.where;
				$.getJSON( sSource, queryParams, function (json) { 
					response.iTotalRecords=json["data"][0]["count"];
					//console.log(response);
					//returns converted data to datatable
					fnCallback(response);
				}); //getJSON
			}); //getJSON
		}); //getJSON
    }//serverDataTableCallback
    
    /***
     * initialize and load a datatable
     * Example:
     * 	loadTable($('#documentTable'),documentsDataTableDef,url,dataArray);
     * dataArray is a global array variable that will contain the rows currently displayed
     */
    function loadTable(oDataTable,oTableDef,sUrl,dataArray){
    	oDataTable.dataTable().fnDestroy();
    	oDataTable.attr("style",'width:100% !important');
    	var tDef = $.extend({},oTableDef);
    	if (window.location.protocol == "https:"){
    		sUrl=sUrl.replace("http:","https:");
    	}
    	tDef.sAjaxSource= sUrl;
    	tDef.bProcessing = true,
    	tDef.bServerSide = true,
    	tDef.oLanguage = {sProcessing:"Loading data from BaasBox, please wait..."};
    	tDef.fnServerData= function ( sSource, aoData, fnCallback ) {serverDataTableCallback( sSource, aoData, fnCallback,dataArray)},
    	oDataTable.dataTable(tDef);
    }//loadTable
    
    function resetDataTable(oDataTable){
    	var oSettings = oDataTable.dataTable().fnSettings();
    	oSettings.oFeatures.bServerSide = false;
    	var iTotalRecords = oSettings.fnRecordsTotal();
    	for (i=0;i<=iTotalRecords;i++) {
    		oDataTable.dataTable().fnDeleteRow(0,null,true);
    	}
    }