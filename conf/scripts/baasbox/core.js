/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by Andrea Tortorella on 23/06/14.
 */

//Console.log("Loaded baasbox core");

/**
 * Baasbox server version
 * @type {string}
 */
exports.version = '0.9.2';


//-------- WS (Remote API calls) --------

var WS = {};
var wsRequest = function(method,url,body,params,headers){
    return _command({resource: 'script',
                     name: 'ws',
                     params: {
                         url: url,
                         method: method,
                         params: params,
                         headers: headers,
                         body: body
                     }});
};

WS.post = function(url,body,opts){
    opts =opts||{};
    return wsRequest('post',url,body,opts.params,opts.headers);
};

WS.get = function(url,opts){
    opts=opts||{};
    return wsRequest('get',url,null,opts.params,opts.headers);
};

WS.put = function(url,body,opts){
    opts = opts||{};
    return wsRequest('put',url,body,opts.params,opts.headers);
};


WS.delete = function(url,opts){
    opts = opts||{};
    return wsRequest('delete',url,null,opts.params,opts.headers);
};
//-------- END WS --------



var log = function(){
    if (arguments.length < 1){
        return;
    }

    var message = arguments[0];
    var args = Array.prototype.slice.call(arguments,1);

    _command({resource: 'script',
              name: 'log',
              params: {message: message,args:args}});

};


//-------- DB --------

var DB = {};

DB.isAnId=function(id){
    return _command({resource: 'db',
        name: 'isAnId',
        params: {
        	id:id
        	}
        });
};

DB.isInTransaction = function(){
    return _command({resource: 'db',
                     name: 'isInTransaction'});
};

DB.beginTransaction = function(){
  _command({resource: 'db', name:'beginTransaction'});
};

DB.commit = function(){
    _command({resource: 'db', name: 'commitTransaction'});
};

DB.rollback = function(){
    _command({resource: 'db', name: 'rollbackTransaction'});
    return null;
};

var ABORT  = Object.create(null);
DB.ABORT = ABORT;

DB.runInTransaction = function(fn){
    try {
        DB.beginTransaction();
        var r =fn(ABORT);
        if(DB.isInTransaction()) {
            if (r === ABORT) {
                DB.rollback();
            } else {
                DB.commit();
            }
        }
        return r;
    } catch (x){
        if (DB.isInTransaction()){
            DB.rollback();
        }
        throw x;
    }
};

DB.createCollection = function(name){
    if(! (typeof name === 'string')){
        throw new TypeError("missing collection name");
    }
    return _command({resource: 'collections',
                     name: 'post',
                     params: name});
};

DB.dropCollection = function(name){
    if(!(typeof name === 'string')) {
         throw new TypeError("missing collection name");
    }
    return _command({resource: 'collections',
                     name: 'drop',
                     params: name})

};
DB.existsCollection = function(name){
    if(!(typeof name === 'string')){
        throw new TypeError("missing collection name");
    }
    return _command({resource: 'collections',
                     name: 'exists',
                     params: name});
};

DB.ensureCollection = function(name){
    if(!DB.existsCollection(name)){
        return DB.createCollection(name);
    } else {
        return true;
    }
};
//-------- END DB --------



var isAdmin = function(){
    return _command({resource: 'db',
                     name: 'isAdmin'});
};

var runAsAdmin = function(fn) {
    return _command({resource: 'db',
                     name: 'switchUser',
                     callback: fn});
};

//-------- Users --------

var Users = {};
Users.find = function(){
    var q = null,
        id = null;
    if(arguments.length < 1) {
        throw new TypeError("missing parameter");
    } else if(typeof arguments[0] === 'string') {
        id = arguments[0];
    } else if(typeof arguments[0] === 'object'){
        q = arguments[0];
    }
    if(!!id){
        return _command({resource: 'users',
                         name: 'get',
                         params: {
                             username: id
                         } });
    } else if(!!q) {
        return _command({resource: 'users',
                         name: 'list',
                        params: q});
    } else {
        throw new TypeError("you must specify  a username or a query")
    }
};

Users.followers = function(us){
    var user = us||context.userName;
    var q = arguments.length>1?arguments[1]:null;
    return _command({resource: 'users',
                     name: 'followers',
                     params: {
                         user: user,
                         query: q
                     }});
};

Users.following = function(us){
    var user = us||context.userName;
    var q = arguments.length>1?arguments[1]:null;
    return _command({resource: 'users',
                     name: 'following',
                     params: {
                         user: user,
                         query: q
                     }});
};


Users.follow = function(){
    var from,to;
    if(arguments.length>1){
        if(!isAdmin()) return null;
        from = arguments[0];
        to = arguments[1];
    } else if(arguments.length==1){
        if(isAdmin()) return null;
        from = context.userName;
        to = arguments[0];
    }
    return _command({resource: 'users',
              name: 'follow',
              params: {
                  from: from,
                  to: to,
                  remove: false
              }});
};

Users.unfollow = function(){
    var from,to;
    if(arguments.length>1){
        if(!isAdmin()) return null;
        from = arguments[0];
        to = arguments[1];
    } else if(arguments.length==1){
        if(isAdmin()) return null;
        from = context.userName;
        to = arguments[0];
    }
    return _command({resource: 'users',
        name: 'follow',
        params: {
            from: from,
            to: to,
            remove: true
        }});
};

Users.create = function(){
    var usr,
        pass,
        role,
        visibleByAnonymousUsers,
        visibleByRegisteredUsers,
        visibleByTheUser,
        visibleByFriends;
    usr = pass = role = visibleByAnonymousUsers =
        visibleByFriends = visibleByTheUser = visibleByRegisteredUsers = null;
    switch (arguments.length){
        case 4:
            visibleByFriends = arguments[3].visibleByFriends;
            visibleByRegisteredUsers= arguments[3].visibleByRegisteredUsers;
            visibleByTheUser = arguments[3].visibleByTheUser;
            visibleByAnonymousUsers= arguments[3].visibleByAnonymousUsers;
        case 3:
            role = arguments[2];
        case 2:
            pass = arguments[1];
            usr = arguments[0];
            break;
        case 1:
            throw new TypeError("missing password");
            break;
        default:
            throw new TypeError("wrong arguments");
    }
    if(usr==null|| (!typeof  usr === 'string')) throw new TypeError("username must be a string");
    if(pass==null||(!typeof pass === 'string')) throw new TypeError("password must be a string");
    if(role != null && (!typeof  role === 'string')) throw new TypeError("role must be a string");
    return _command({resource: 'users', name: 'post',
                     params: {username: usr,
                              password: pass,
                              role: role,
                              visibleByTheUser: visibleByTheUser,
                              visibleByAnonymousUsers: visibleByAnonymousUsers,
                              visibleByRegisteredUsers: visibleByRegisteredUsers,
                              visibleByFriends: visibleByFriends}});
};

Users.me = function(){
    return Users.find(context.userName);
};

Users.save = function(uzr){
    var upd = {};
    if(arguments.length == 1 && typeof arguments[0] === 'object') {
        upd.visibleByFriends = uzr.visibleByFriends;
        upd.visibleByAnonymousUsers = uzr.visibleByAnonymousUsers;
        upd.visibleByRegisteredUsers = uzr.visibleByRegisteredUsers;
        upd.visibleByTheUser = uzr.visibleByTheUser;

        if(isAdmin()) {
        	//admin can update any user
            upd.username = uzr.username;
            //admin can update the role as well
            upd.role = uzr.role;
        } else {
        	if (uzr.role!=null) throw new TypeError("Only administrators can update a user role");
        	if (uzr.username!=null && uzr.username!==context.userName) throw new TypeError("Users can update only their own profiles");
            upd.username = context.userName;
        }

        return _command({resource: 'users',
            name: 'put',
            params: upd
        });

    } else {
        throw new TypeError("you must supply a user to save");
    }
};

/*
 * Users.changePassword(username,newpass);
 */
Users.changePassword = function(username,password){
	if (arguments.length!=2) throw new TypeError("Users.changePassword() needs 2 arguments");
	if(!isAdmin() && context.userName!=username) {
		throw new TypeError("You have to be an administrator to change someone else password");
	} 
    return _command({resource: 'users',
        name: 'changePassword',
        params: {
        	username:username,
        	newPassword:password
        }
    });
};

/*
 * Users.changeUsername(username,newUsername);
 */
Users.changeUsername = function(username,newUsername){
	if (arguments.length!=2) throw new TypeError("Users.changeUsername() needs 2 arguments");
	if(!isAdmin() && context.userName!=username) {
		throw new TypeError("You have to be an administrator to change someone else username");
	} 
    return _command({resource: 'users',
        name: 'changeUsername',
        params: {
        	username:username,
        	newUsername:newUsername
        }
    });
};
//-------- END Users --------


//--------   Documents --------
var Documents = {};
Documents.find = function(){
    var coll = null,
        q = null,
        id = null;
    switch (arguments.length){
        case 2:
            if(typeof arguments[1] === 'string') {
                id = arguments[1];
            } else {
                q = arguments[1];
            }
        //fall through (missing break)
        case 1:
            coll = arguments[0];
    }
    if(!(typeof coll === 'string')){
        throw new TypeError("you must specify a collection");
    }
    if(id === null ){
        return _command({resource: 'documents',
                         name: 'list',
                         params: {
                             collection: coll,
                             query: q
                         }});
    } else {
        return _command({resource: 'documents',
                         name: 'get',
                         params:{
                             collection: coll,
                             id: id
                         }});
    }
};

Documents.remove = function(coll,id){
    if(!(coll && id)){
        throw new TypeError("missing arguments");
    }
    return _command({resource: 'documents',
                     name: 'delete',
                     params: {

                         collection: coll,
                         id: id
                     }});
};

Documents.revoke = function(coll,id,permissions){
    if (! typeof permissions === 'object'){
        throw new TypeError("invalid permissions")
    }
    return _command({resource: 'documents',
        name: 'revoke',
        params: {
            collection: coll,
            id: id,
            users: permissions.users,
            roles: permissions.roles,
        }});
};

Documents.grant = function(coll,id,permissions){
    if (! typeof permissions === 'object'){
        throw new TypeError("invalid permissions")
    }
    return _command({resource: 'documents',
        name: 'grant',
        params: {
            collection: coll,
            id: id,
            users: permissions.users,
            roles: permissions.roles
        }});
};



Documents.save = function(){
    var coll = null,
        obj = null,
        id = null,
        author = null;
    if(arguments.length===1 && typeof arguments[0] === 'object'){
        obj = arguments[0];
        coll = obj['@class'];
        id = obj['id'];
        author = obj['_author'];
    } else if(arguments.length===2 &&
              typeof arguments[0]==='string' &&
              typeof arguments[1]==='object'){
        coll = arguments[0];
        obj = arguments[1];
        id = obj['id'];
        author=obj['_author'];
    }
    if(!(obj && coll)){
        throw new TypeError("Invalid arguments");
    }
    if(id){
        return _command({resource: 'documents',
                         name: 'put',
                         params: {
                             collection: coll,
                             data: obj,
                             id: id
                         }});
    } else {
        return _command({
            resource: 'documents',
            name: 'post',
            params: {
                collection: coll,
                author: author,
                data: obj
            }
        });
    }
};

//-------- End Documents --------

var queryUsers = function(to){
    var ret = [];
    Users.find(to).forEach(function (u){
      ret.push(u.username);
    });
    return ret;
};

//------------Push----------------
var Push ={};
Object.defineProperty(Push,"OK",{value: 0});
Object.defineProperty(Push,"ERROR",{value: 2});
Object.defineProperty(Push,"PARTIAL",{value: 1});
Object.defineProperty(Push,"PROFILE_1",{value: 1});
Object.defineProperty(Push,"PROFILE_2",{value: 2});
Object.defineProperty(Push,"PROFILE_3",{value: 3});

Push.send = function(){
    var body,
        to,
        profiles=Push.PROFILE_1;

    switch (arguments.length){
        case 3:
            profiles = arguments[2];
        case 2:
            body = arguments[1];
            to = arguments[0];
            break;
        default:
            throw new TypeError("missing required parameters");
    }
    if(body === null||to===null){
        throw new TypeError("missing required parameters body and to");
    }
    if(typeof body === 'string'){
        body = {message: body};
    }

    if(typeof to === 'string'){
        to = [to];
    } else if((Object.prototype.toString.apply(to) === '[object Object]')){
        if(to.username){
            to=to.username;
        } else {
            to = queryUsers(to);
        }
    } else if(!(Object.prototype.toString.apply(to) === '[object Array]')){
        throw new TypeError("wrong to parameter");
    }

    if(typeof profiles === 'number'){
        profiles = [profiles];
    }

    return _command({resource: 'push',
                     name: 'send',
                     params:{
                        'body': body,
                         'to': to,
                         'profiles': profiles
                     }});
};
//---------- END Push --------

//---------- Links -----------
var Links = {};

//find({search params}), find(id)
Links.find = function(){
	if (arguments.length===0) {
		throw new TypeError("Links.find() need one parameter");
	}
	var q = null,
      id = null;
	if(typeof arguments[0] === 'string') {
		id = arguments[0];
	} else {
	    q = arguments[0];
	}
  if(id === null ){
      return _command({resource: 'links',
                       name: 'list',
                       params: {
                           query: q
                       }});
  } else {
      return _command({resource: 'links',
                       name: 'get',
                       params:{
                           id: id
                       }});
  }
};

Links.remove = function(id){
  if(!id){
      throw new TypeError("Links.remove() need one arguments");
  }
  return _command({resource: 'links',
                   name: 'delete',
                   params: {
                       id: id
                   }});
};

//save({params})
Links.save = function(params){
  var label = null,
      id = null,
		sourceId=null,
		destId=null;
	if (!params ||  typeof params!='object'){
		throw new TypeError("Invalid arguments: Links.save() need an object as parameter");
	}
		
	id = params['id'];
	if (id) {
		throw new TypeError("Invalid arguments: Links.save() does not allow to update links. You passed an id: " + id);
	}
	sourceId = params['sourceId'];
	destId = params['destId'];
	label = params['label'];
	
	if (!sourceId || !destId || !label){
		throw new TypeError("Invalid arguments: Links.save() need an object as parameter with following parameters {sourceId,destId,label}");
	}

  return _command({
     resource: 'links',
     name: 'post',
     params: {
       sourceId: sourceId,
       destId: destId,
       label: label
     }
  });
  
};
//---------- END Links ------

exports.Documents = Documents;
exports.Users = Users;
exports.DB = DB;
exports.Push = Push;
exports.WS= WS;
exports.log = log;
exports.Links = Links;

exports.runAsAdmin=runAsAdmin;

//exports.runInTransaction=runInTransaction;

exports.isAdmin=isAdmin;

//exports.isInTransaction=isInTransaction;
