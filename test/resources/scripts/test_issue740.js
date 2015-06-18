

http().get(function(req){
    Box.runAsAdmin(function(){
    	 Box.DB.ensureCollection("Country");
    	 Box.DB.dropCollection("Country");
    	 Box.DB.createCollection("Country");
	    Box.DB.exec("create property Country.EN STRING"); //This is works
	    Box.DB.exec("alter property Country.EN collate ci"); //Exception
    });
    return {status: 200};    
});

