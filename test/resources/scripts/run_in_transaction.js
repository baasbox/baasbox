/**
 * Created by eto on 06/10/14.
 */

http().post(function(e){
    var coll = e.body.collection;
    var op = e.body.op;
    var doc = null;
    if('normal' == op) {
          try{
              Box.DB.beginTransaction();
              doc =Box.Documents.save(coll,{doc: 'dd'});
              Box.Documents.grant(coll,doc.id,{roles: {read: ['anonymous']}});
              Box.DB.commit();

          }catch (e){
              Box.DB.rollback();
              throw e;
          }

        return {status: 200, content: doc};
    } else{
        return {status: 400, content: 'wrong operation'};
    }

});