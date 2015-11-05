http().get(function(req){
    var collectionName = req.queryString.postCollection[0];
    var postId = req.queryString.postId[0];
    var linksObject = {linkName:'comment',linkDir:'out'};
    if(req.queryString.where){
    	linksObject.where = req.queryString.where[0]
    	 if(req.queryString.params){
    	    	linksObject.params = req.queryString.params 
    	    }
    }
   
    var result = Box.Documents.Links.find(collectionName,postId,{links:linksObject})
    if(result){
    	return {status: 200,content:result};
    }
});