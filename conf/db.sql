
--classes
--Node
create class NodeVertex extends ographvertex;


create class Node abstract extends ORestricted;
create property NodeVertex._node link Node;
create property Node._creation_date datetime;
create property Node._links link NodeVertex;


--user
create class User extends Node;
create class UserAttributes extends ORestricted;
create property User.visibleByAnonymousUsers link UserAttributes;
create property User.visibleByRegisteredUsers link UserAttributes;
create property User.visibleByFriend link UserAttributes;
create property User.visibleByTheUser link UserAttributes;
create property User._audit embedded;
create property User.user link ouser;


--admin user
insert into user set user = (select from ouser where name='admin'), _links = (insert into nodevertex set _node=null);
update nodevertex set _node=(select from user where user.name='admin');
 
--users' constraints
alter property NodeVertex._node mandatory=true;
alter property NodeVertex._node notnull=true;
alter property Node._creation_date mandatory=true;
alter property Node._creation_date notnull=true;
alter property User.user mandatory=true;
alter property User.user notnull=true;
alter property Node._links mandatory=true;
alter property Node._links notnull=true;

--storage
create class Collection extends Node;
create property Collection.name String;
alter property Collection.name mandatory=true;
alter property Collection.name notnull=true;


create class File extends Node;
create property File.name String;
alter property File.name mandatory=true;
alter property File.name notnull=true;
create property File.internalName String;
alter property File.internalName mandatory=true;
alter property File.internalName notnull=true;
create property File.contentType String;
alter property File.contentType mandatory=true;
alter property File.contentType notnull=true;
create property File.content LINKLIST ;


create class Post extends Node;
create property Post.content String;
alter property Post.content mandatory=true;
alter property Post.content notnull=true;

create class Message extends Node;
create class Notification extends Node;

create class Asset extends Node;
create class FileAsset extends Asset;
create property Asset.name String;
alter property Asset.name mandatory=true;
alter property Asset.name notnull=true;
create property FileAsset.fileName String;
alter property FileAsset.fileName mandatory=true;
alter property FileAsset.fileName notnull=true;
create property FileAsset.contentType String;
alter property FileAsset.contentType mandatory=true;
alter property FileAsset.contentType notnull=true;
create property FileAsset.contentLength long;
alter property FileAsset.contentLength mandatory=true;
alter property FileAsset.contentLength notnull=true;
create property FileAsset.file link;
alter property FileAsset.file mandatory=true;
alter property FileAsset.file notnull=true;

--Edges
create class Created extends ographedge;

--social interaction
create class Friendship extends ographedge;
create property Friendship.fromDate datetime;
alter property Friendship.fromDate mandatory=true;
alter property Friendship.fromDate notnull=true;
create property Friendship.requestDate datetime;
alter property Friendship.requestDate mandatory=true;
alter property Friendship.requestDate notnull=true;

create class Comment extends ographedge;
create class Like extends ographedge;


--analytics
--create class call;

--indices
create index ouser.name unique;
create index collection.name unique;
create index asset.name unique;

--configuration
create index bb_password_recovery dictionary
create index bb_application dictionary

