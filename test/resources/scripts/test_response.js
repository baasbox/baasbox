http().get(function(req){
    var what = req.queryString["what"][0];
    switch(what) {
    case "object":
        return {status: 200,content:{"k":"v",n:1}};    
        break;
    case "collection":
        return {status: 200,content:["hello","world",42,{"k":"v",o:3},true]};    
        break;
    case "null":
        return {status: 200,content:null};    
        break;
    case "nothing":
        return {status: 200};    
        break;
    case "string":
        return {status: 200,content:"Hello World!"}; 
        break;
    case "empty_string":
        return {status: 200,content:""}; 
        break;
    case "string_quote":
        return {status: 200,content:'Hello "World!"'}; 
        break;
    case "number":
        return {status: 200,content:42}; 
        break;
    case "decimal":
        return {status: 200,content:45.98};    
        break;
    case "negative":
        return {status: 200,content:-45.98};    
        break;
    case "exp":
        return {status: 200,content:234e9};    
        break;
    case "infinity":
        return {status: 200,content:234e956};    
        break;
    case "boolean":
        return {status: 200,content:false};  
        break;""
    }
});