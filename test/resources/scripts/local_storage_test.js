/**
 * Created by eto on 08/10/14.
 */

http().post(function (e){

    var st =storage.swap(function (c){
        if(c){
            c.val +=1;
        } else{
            c= {val:0};
        }
        return c;
    });

    return {status: 200,content:st};

}).get(function (e){
    var st =storage.get();

    return {status: 200,content: st}
});