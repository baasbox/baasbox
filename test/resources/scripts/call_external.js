/**
 * Created by eto on 08/10/14.
 */
http().get(function(r){
   var result =Box.WS.get("http://www.google.com");

    return {status: 200,content: result};
});