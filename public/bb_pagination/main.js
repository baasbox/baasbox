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
    
    $('#documentTable').dataTable( {
    	"bProcessing": true,
        "bServerSide": true,
        "sAjaxSource": BBRoutes.com.baasbox.controllers.Document.getDocuments("test").absoluteURL(),
        "fnServerData": function ( sSource, aoData, fnCallback ) {
        	//sSource: URL
        	//aoData: array di parametri
        	console.log("sSource");
        	console.log(sSource);
            console.log("aoData");
        	console.log(aoData);
        	
        	queryParams = new Array();
        	queryParams.push({"page":"1","recordsPerPage":"10"});
			
			$.getJSON( sSource, queryParams, function (json) { 
				console.log(json["data"]);
				/* Do whatever additional processing you want on the callback, then tell DataTables */
				fnCallback(json["data"]);
			} );
        	
        },
        
        "sDom": "R<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span12'i><'span12 center'p>>",
        //"sPaginationType": "bootstrap",
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
                          //return getActionButton("edit","document",data + obj["@class"]) + "&nbsp;" + getActionButton("delete","document",data+obj["@class"]);
                      },bSortable:false,bSearchable:false
                      }
                     ],
        "bRetrieve": true,
        "bDestroy":true
    } );
    

    /*
    BBRoutes.com.baasbox.controllers.Document.getDocuments("test").ajax({
        data: {orderBy: "name asc"},
        success: function(data) {
            data=data["data"];
            $('#documentTable').dataTable().fnClearTable();
            $('#documentTable').dataTable().fnAddData(data);
        }
    })*/
    
} );//ready()

