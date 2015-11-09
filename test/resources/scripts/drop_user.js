http().post(function(req){
	var username = req.queryString.username[0];
	var dropped = Box.Users.remove(username);
	return {status:200,content:dropped}
});