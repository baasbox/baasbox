http().put(function(req) {
    var toFollow =req.body.toFollow;
    var newFriend = Box.Users.follow(toFollow);
    return {status: 200,content: newFriend};
}).delete(function(req) {
    var toUnfollow = req.body.toFollow;
    var res = Box.Users.unfollow(toUnfollow);
    return {status: 200, content: res};
});
