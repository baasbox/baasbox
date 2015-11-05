http().get(function(req){
    var collectionName = String(req.queryString.postCollection);
    var postId = String(req.queryString.postId);
    var result = Box.Documents.find(collectionName,postId,{links:{linkName:'comment',linkDir:'out',where:null}})
    if(result){
    	return {status: 200,content:result};
    }
});