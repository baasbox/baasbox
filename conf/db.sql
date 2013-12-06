--classes
--Node
create class _BB_NodeVertex extends V;

--Node class should be abstract but we cannot declare it as abstrat due the index on the id field
create class _BB_Node  extends ORestricted;
create property _BB_NodeVertex._node link _BB_Node;
create property _BB_Node._creation_date datetime;
create property _BB_Node._links link _BB_NodeVertex;
create property _BB_Node.id String;

--user
create class _BB_User extends _BB_Node;
create class _BB_UserAttributes extends ORestricted;
create property _BB_User.visibleByAnonymousUsers link _BB_UserAttributes;
create property _BB_User.visibleByRegisteredUsers link _BB_UserAttributes;
create property _BB_User.visibleByFriend link _BB_UserAttributes;
create property _BB_User.visibleByTheUser link _BB_UserAttributes;
create property _BB_User._audit embedded;
create property _BB_User.user link ouser;


--admin user
insert into _BB_User set user = (select from ouser where name='admin'), _links = (insert into _BB_NodeVertex set _node=null), _creation_date = sysdate(), signUpDate = sysdate();
update _BB_NodeVertex set _node=(select from _BB_User where user.name='admin');
 
--reset pwd
create class _BB_ResetPwd;

--users' constraints
alter property _BB_NodeVertex._node mandatory=true;
alter property _BB_NodeVertex._node notnull=true;
alter property _BB_Node._creation_date mandatory=true;
alter property _BB_Node._creation_date notnull=true;
alter property _BB_User.user mandatory=true;
alter property _BB_User.user notnull=true;
alter property _BB_Node._links mandatory=true;
alter property _BB_Node._links notnull=true;

--object storage
create class _BB_Collection extends _BB_Node;
create property _BB_Collection.name String;
alter property _BB_Collection.name mandatory=true;
alter property _BB_Collection.name notnull=true;

--files
create class _BB_File extends _BB_Node;
create property _BB_File.fileName String;
alter property _BB_File.fileName mandatory=true;
alter property _BB_File.fileName notnull=true;
create property _BB_File.contentType String;
alter property _BB_File.contentType mandatory=true;
alter property _BB_File.contentType notnull=true;
create property _BB_File.contentLength long;
alter property _BB_File.contentLength mandatory=true;
alter property _BB_File.contentLength notnull=true;
create property _BB_File.file link;
alter property _BB_File.file mandatory=true;
alter property _BB_File.file notnull=true;

--Assets
create class _BB_Asset extends _BB_Node;
create class _BB_FileAsset extends _BB_Asset;
create property _BB_Asset.name String;
alter property _BB_Asset.name mandatory=true;
alter property _BB_Asset.name notnull=true;
create property _BB_FileAsset.fileName String;
alter property _BB_FileAsset.fileName mandatory=true;
alter property _BB_FileAsset.fileName notnull=true;
create property _BB_FileAsset.contentType String;
alter property _BB_FileAsset.contentType mandatory=true;
alter property _BB_FileAsset.contentType notnull=true;
create property _BB_FileAsset.contentLength long;
alter property _BB_FileAsset.contentLength mandatory=true;
alter property _BB_FileAsset.contentLength notnull=true;
create property _BB_FileAsset.file link;
alter property _BB_FileAsset.file mandatory=true;
alter property _BB_FileAsset.file notnull=true;

--indices
create index ouser.name unique;
create index _BB_Collection.name unique;
create index _BB_asset.name unique;
create index _BB_Node.id unique;

--configuration class
create class _BB_Index;
create property _BB_Index.key String;
alter property _BB_Index.key mandatory=true;
alter property _BB_Index.key notnull=true;
create index _BB_Index.key unique;




