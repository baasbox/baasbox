http().get(function(req){
    var cacheKey = req.queryString.key;
    var cacheType = req.queryString.cacheType || 'global'
    if(!cacheKey){
        return {status:400,content:'key must be specified'}
    }
    var result = Box.Cache.getValueOrElse(cacheType,cacheKey,function(key){
        return {status:404}
    })
    if(result){
    	return {status: 200,content:result};
    }
}).post(function(req){
    var cacheType = req.body.cacheType || 'global'
    var cacheKey = req.body.key;
    var cacheValue = req.body.value;
    if(!cacheKey || !cacheValue) {
        return {status:400,content:'key and value must be specified'}
    }else{
        Box.Cache.setValue(cacheType,cacheKey,cacheValue);
        return {status: 200};    
    }
    
});