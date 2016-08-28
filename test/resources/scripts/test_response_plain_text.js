http().get(function(req){
    var what = req.queryString["what"][0];
    var headers={"Content-Type":"text/plain"};
    
    switch(what) {
    case "object":
        return {status: 200,content:{"k":"v",n:1},headers:headers};    
        break;
    case "collection":
        return {status: 200,content:["hello","world",42,{"k":"v",o:3},true],headers:headers};   
        break;
    case "null":
        return {status: 200,content:null,headers:headers};   
        break;
    case "nothing":
        return {status: 200,headers:headers};   
        break;
    case "string":
        return {status: 200,content:"Hello World!",headers:headers};   
        break;
    case "empty_string":
        return {status: 200,content:"",headers:headers};   
        break;
    case "string_quote":
        return {status: 200,content:'Hello "World!"',headers:headers};    
        break;
    case "number":
        return {status: 200,content:42,headers:headers};   
        break;
    case "decimal":
        return {status: 200,content:45.98,headers:headers};      
        break;
    case "negative":
        return {status: 200,content:-45.98,headers:headers};     
        break;
    case "exp":
        return {status: 200,content:234e9,headers:headers};      
        break;
    case "infinity":
        return {status: 200,content:234e956,headers:headers};      
        break;
    case "boolean":
        return {status: 200,content:false,headers:headers};    
        break;""
    }
});