--compatibility with 1.3.0
--alter database custom useLightweightEdges=false;
--alter database custom useClassForEdgeLabel=false;
--alter -database custom useClassForVertexLabel=false;
--alter database custom useVertexFieldsForEdgeLabels=false;

--classes
--Node
create class NodeVertex extends V;

--Node class should be abstract but we cannot declare it as abstrat due the index on the id field
create class Node  extends ORestricted;
create property NodeVertex._node link Node;
create property Node._creation_date datetime;
create property Node._links link NodeVertex;
create property Node.id String;

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
insert into user set user = (select from ouser where name='admin'), _links = (insert into nodevertex set _node=null), _creation_date = sysdate(), signUpDate = sysdate();
update nodevertex set _node=(select from user where user.name='admin');
 
--reset pwd
create class ResetPwd;

--users' constraints
alter property NodeVertex._node mandatory=true;
alter property NodeVertex._node notnull=true;
alter property Node._creation_date mandatory=true;
alter property Node._creation_date notnull=true;
alter property User.user mandatory=true;
alter property User.user notnull=true;
alter property Node._links mandatory=true;
alter property Node._links notnull=true;

--object storage
create class Collection extends Node;
create property Collection.name String;
alter property Collection.name mandatory=true;
alter property Collection.name notnull=true;



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
create class Created extends E;



--indices
create index ouser.name unique;
create index collection.name unique;
create index asset.name unique;
create index Node.id unique;

--configuration
create index bb_password_recovery dictionary;
create index bb_application dictionary;
create index bb_images dictionary;
create index bb_push dictionary;
create index bb_internal dictionary;


