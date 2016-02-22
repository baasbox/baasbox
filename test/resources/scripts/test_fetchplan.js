
http().get(function(req){
    var coll = "fetchplan_test";
    Box.runAsAdmin(function(){
    	Box.DB.ensureCollection(coll);
    });
    
    var doc = Box.Documents.save(coll,{
        "key":"value"
    });
    
    var result1 = Box.Documents.find(coll,doc.id);
    var result2 = Box.Documents.find(coll,{
        where:"id='" + doc.id + "'",
        fetchPlan:"_audit:2,class"
    });
    var result3 = Box.DB.select('from ' + coll + ' where id=? and 1=1',[doc.id]);
    var result4 = Box.DB.select('from ' + coll + ' where id=? and 2=2',[doc.id],{
        fetchPlan:"_audit:2,class"
    });
    
    return {status: 200,content:{
        result1:result1,
        result2:result2,
        result3:result3,
        result4:result4
    }};    
});

