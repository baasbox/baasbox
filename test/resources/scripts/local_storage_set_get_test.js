/**
 * Created by eto on 08/10/14.
 */

http().post(function (e){
    var b = e.body;
    b= b.store?b:null;
    var st =storage.set(b);


    return {status: 200,content: {storage: st}};
}).get(function (e){
    var st =storage.get();
    return {status: 200,content: {storage: st}};
});