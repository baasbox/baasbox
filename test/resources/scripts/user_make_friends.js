http().put(function(req) {
    var toFollow =req.body.toFollow;
    var newFriend =Box.Users.follow(toFollow);
    return {status: 200,content: newFriend};
});
