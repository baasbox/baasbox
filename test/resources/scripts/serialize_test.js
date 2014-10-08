/**
 * Created by eto on 08/10/14.
 */

http().get(function(req){
    var ret = {
        custom: {toJSON: function(){
            return {val: 'custom'};
        }},
        date: new Date(),
        double: 1.0,
        integer: 1,
        text: "text",
        obj: {},
        ary: [],
        no: null
    };



    return {status: 200, content: ret};
});