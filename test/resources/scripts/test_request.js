/**
 * Created by eto on 1/13/15.
 */
http().get(function (req){
    return {status: 200, content: req};
}).post(function (req){
    return {status: 200, content: req};
}).put(function (req){
    return {status: 200,content: req};
}).delete(function (req){
    return {status: 200,content: req};
});