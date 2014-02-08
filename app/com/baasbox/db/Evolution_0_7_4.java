package com.baasbox.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import play.Logger;

import com.baasbox.configuration.Internal;
import com.baasbox.dao.IndexDao;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabase.ATTRIBUTES;
import com.orientechnologies.orient.core.db.ODatabaseComplex;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

/**
 * Evolves the DB to the 0.7.3 schema
 * introducing the "File"s
 * 
 * @author Claudio Tesoriero
 *
 */
public class Evolution_0_7_4 implements IEvolution {
	private String version="0.7.4-snapshot";
	
	public Evolution_0_7_4() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			changePushTokenFieldName(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	private void changePushTokenFieldName(ODatabaseRecordTx db) {
		Logger.info("..changing 'deviceId' to 'pushToken' field name..:");
		UserDao dao = UserDao.getInstance();
		QueryParams criteria = QueryParams.getInstance();
		try {
			List<ODocument> users = UserService.getUsers(criteria);
			for (ODocument user:users){
				ODocument userSystemProperties = user.field(UserDao.ATTRIBUTES_SYSTEM);
				if (userSystemProperties!=null){
					List<ODocument> loginInfos=userSystemProperties.field(UserDao.USER_LOGIN_INFO);
					for(ODocument loginInfo : loginInfos){
						String deviceId=loginInfo.field("deviceId");
						loginInfo.field(UserDao.USER_PUSH_TOKEN, deviceId);
						loginInfo.save();
					}
					userSystemProperties.save();
				}
				user.save();
			}
		} catch (SqlInjectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		Logger.info("...done...");
	}
		
	private void fileClassCreation(ODatabaseRecordTx db) {
		Logger.info("..creating _BB_File class..:");
		String[] script=new String[]{
		"create class _BB_File extends _BB_Node;",
		"create property _BB_File.fileName String;",
		"alter property _BB_File.fileName mandatory=true;",
		"alter property _BB_File.fileName notnull=true;",
		"create property _BB_File.contentType String;",
		"alter property _BB_File.contentType mandatory=true;",
		"alter property _BB_File.contentType notnull=true;",
		"create property _BB_File.contentLength long;",
		"alter property _BB_File.contentLength mandatory=true;",
		"alter property _BB_File.contentLength notnull=true;",
		"create property _BB_File.file link;",
		"alter property _BB_File.file mandatory=true;",
		"alter property _BB_File.file notnull=true;",
		"create class _BB_File_Content;",
		"create property _BB_File_Content.content String;",
		"create index _BB_File_Content.content.key FULLTEXT_HASH_INDEX;"};
		for (String line:script){
			if (Logger.isDebugEnabled()) Logger.debug(line);
			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
			}
		} 
		Logger.info("...done...");
	}



}
