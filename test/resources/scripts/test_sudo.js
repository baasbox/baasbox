http().post(function (e){
    var coll = e.body.coll;

    var res =Box.runAsAdmin(function (){
        var isam =Box.isAdmin();
        var c =Box.DB.ensureCollection(coll);
        return {adm: isam,coll: c};
    });

    var myself =Box.Users.me();
    var exists =Box.DB.existsCollection(coll);
    return {status: 200, content: {block: res,us: myself,exists: exists}};

});