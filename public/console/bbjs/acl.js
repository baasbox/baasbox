
/***
 * "Rotate" a ACL Json. Es:
 * {"_allowRead":
	[
		{"name":"registered","isRole":true},
		{"name":"user1"}
	]
}
becomes something like
{
	"roles":[
		{"registered":["_allowRead","_allowUpdate"]},
		{"writer":["_allowRead"]}
	],
	"users":[
		{"user1":["_allowRead","_allowUpdate"]}
	],

}
 * @param aclJson
 */
function rotateAcl(aclJson){
	var rotatedJson = {"roles":[],"users":[]};
	for (aclJson.)
}