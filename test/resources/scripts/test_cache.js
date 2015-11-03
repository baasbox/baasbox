http().get(function(req){
    var cacheKey = req.queryString.key;
    var cacheScope= req.queryString.cacheScope || 'app'
    if(!cacheKey){
        return {status:400,content:'key must be specified'}
    }
    var result = Box.Cache.getOrElse(cacheKey,{scope:cacheScope,callback:function(key){
        return {status:404}
    }})
    if(result){
    	return {status: 200,content:result};
    }
}).post(function(req){
    var cacheScope = req.body.cacheScope || 'app'
    var cacheKey = req.body.key;
    var cacheValue = req.body.value;
    if(!cacheKey || !cacheValue) {
        return {status:400,content:'key and value must be specified'}
    }else{
        Box.Cache.set(cacheKey,cacheValue,{scope:cacheScope});
        return {status: 200};    
    }
    
}).delete(function(req){
    var cacheScope = req.queryString.cacheScope || 'app';
    var cacheKey = req.queryString.key;
    if(!cacheKey) {
        return {status:400,content:'key must be specified'}
    }else{
        Box.Cache.remove(cacheKey,{scope:cacheScope});
        return {status: 200};    
    };
});