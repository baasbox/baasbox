--database settings
alter database DATETIMEFORMAT yyyy-MM-dd'T'HH:mm:ss.SSSZ
alter database custom useLightweightEdges=false
alter database custom useClassForEdgeLabel=false
alter database custom useClassForVertexLabel=true
alter database custom useVertexFieldsForEdgeLabels=true

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

create class _BB_File_Content;
create property _BB_File_Content.content String;
create index _BB_File_Content.content.key FULLTEXT_HASH_INDEX;


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

--permissions
create class _BB_Permissions;
create property _BB_Permissions.tag String;
create property _BB_Permissions.enabled boolean;
alter property _BB_Permissions.tag mandatory=true;
alter property _BB_Permissions.tag notnull=true;
alter property _BB_Permissions.enabled mandatory=true;
alter property _BB_Permissions.enabled notnull=true;

create property orole.isrole boolean
update orole set isrole=true

--indices

alter property ouser.name collate ci;
create index _BB_Collection.name unique;
create index _BB_asset.name unique;
create index _BB_Node.id unique;
create index _BB_Permissions.tag unique;
---bug on OrientDB index? (our issue #412) We have to define a "new" index to avoid class scan when looking for a username:
create index _bb_user.user.name unique

--configuration class
create class _BB_Index;
create property _BB_Index.key String;
alter property _BB_Index.key mandatory=true;
alter property _BB_Index.key notnull=true;
create index _BB_Index.key unique;

--LINKS
create property E.id String;
alter property E.id notnull=true;
create index E.id unique;

--Scripts
create class _BB_Script;
create property _BB_Script.name String;
alter property _BB_Script.name mandatory=true;
alter property _BB_Script.name notnull=true;
create property _BB_Script.code embeddedlist string;
alter property _BB_Script.code mandatory=true;
alter property _BB_Script.code notnull=true;
create property _BB_Script.lang string;
alter property _BB_Script.lang mandatory=true;
alter property _BB_Script.lang notnull=true;
create property _BB_Script.library boolean;
alter property _BB_Script.library mandatory=true;
alter property _BB_Script.library notnull=true;
create property _BB_Script.active boolean;
alter property _BB_Script.active mandatory=true;
alter property _BB_Script.active notnull=true;
create property _BB_Script._storage embedded;
create property _BB_Script._creation_date datetime;
create property _BB_Script._invalid boolean;
alter property _BB_Script._invalid mandatory=true;
alter property _BB_Script._invalid notnull=true;
create index _BB_Script.name unique;
