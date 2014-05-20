/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
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

package com.baasbox.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

import com.baasbox.configuration.PasswordRecovery;
import com.baasbox.dao.exception.ResetPasswordException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class ResetPwdDao extends NodeDao  {

	private final static String MODEL_NAME="_BB_ResetPwd";
	private final static String USER_LINK = "user";
	public final static String ATTRIBUTES_BASE64CODE_STEP1="base64_code_step1";
	public final static String ATTRIBUTES_BASE64CODE_STEP2="base64_code_step2";
	public final static String ATTRIBUTES_REQUEST_DATE="request_date";
	public final static String ATTRIBUTES_COMPLETED_DATE="completed_date";
	public final static String ATTRIBUTES_INVALID = "canceled";  //used if a new request for a password reset arrives. The old one becomes invalid 
	
	protected ResetPwdDao() {
		super(MODEL_NAME);
	}

	public static ResetPwdDao getInstance(){
		return new ResetPwdDao();
	}
	
	public ODocument create(Date request_date, String base64code, ODocument user) {
		
		//invalidates previous token associated with the user
		String sql = "update " + MODEL_NAME + " set "+ATTRIBUTES_INVALID+"=true where user.user.name=? and "+ATTRIBUTES_COMPLETED_DATE+" is null";
		GenericDao.getInstance().executeCommand(sql, new Object[] {((ODocument)user.field("user")).field("name")});
		 
		ODocument doc = new ODocument(MODEL_NAME);
		doc.field(FIELD_CREATION_DATE, new Date());
		doc.field(USER_LINK,user.getIdentity());
		doc.field(ATTRIBUTES_BASE64CODE_STEP1, base64code);
		doc.field(ATTRIBUTES_REQUEST_DATE, request_date);
		doc.field(ATTRIBUTES_INVALID, false);
		
		doc.save();
		return doc;
	}
	
	
	public boolean verifyTokenStep1(String base64code, String username) {
		String timeBeforeExpiration = String.valueOf(PasswordRecovery.EMAIL_EXPIRATION_TIME.getValueAsInteger()*60*1000);
		
		ODocument result=null;
		QueryParams criteria = QueryParams.getInstance().where("user.user.name=? and " + ATTRIBUTES_BASE64CODE_STEP1 + "=? and " + ATTRIBUTES_COMPLETED_DATE + " is null and (" + ATTRIBUTES_INVALID + " is not null or " + ATTRIBUTES_INVALID + " is false) and " + ATTRIBUTES_REQUEST_DATE + " > ( sysdate() - ? )").params(new String [] {username, base64code, timeBeforeExpiration});
		List<ODocument> resultList;
		try {
			resultList = super.get(criteria);
		} catch (SqlInjectionException e) {
			throw new RuntimeException(e);
		}
		if (resultList!=null && resultList.size()>0) result = resultList.get(0);
		return result!=null;
	}
	
	public boolean verifyTokenStep2(String base64code, String username)  {
		
		String timeBeforeExpiration = String.valueOf(PasswordRecovery.EMAIL_EXPIRATION_TIME.getValueAsInteger()*60*1000);
		
		ODocument result=null;
		QueryParams criteria = QueryParams.getInstance().where("user.user.name=? and " + ATTRIBUTES_BASE64CODE_STEP2 + "=? and " + ATTRIBUTES_COMPLETED_DATE + " is null and (" + ATTRIBUTES_INVALID + " is not null or " + ATTRIBUTES_INVALID + " is false) and " + ATTRIBUTES_REQUEST_DATE + " > ( sysdate() - ? )").params(new String [] {username, base64code, timeBeforeExpiration});
		List<ODocument> resultList;
		try {
			resultList = super.get(criteria);
		} catch (SqlInjectionException e) {
			throw new RuntimeException(e);
		}
		if (resultList!=null && resultList.size()>0) result = resultList.get(0);
		return result!=null;
	}
	
	private ODocument getCurrentResetRecord(String username) throws RuntimeException{
		String timeBeforeExpiration = String.valueOf(PasswordRecovery.EMAIL_EXPIRATION_TIME.getValueAsInteger()*60*1000);
		
		ODocument result=null;
		QueryParams criteria = QueryParams.getInstance().where("user.user.name=? and " + ATTRIBUTES_COMPLETED_DATE + " is null and (" + ATTRIBUTES_INVALID + " is not null or " + ATTRIBUTES_INVALID + " is false) and " + ATTRIBUTES_REQUEST_DATE + " > ( sysdate() - ? )").params(new String [] {username, timeBeforeExpiration});
		List<ODocument> resultList=null;
		try {
			resultList = super.get(criteria);
		} catch (SqlInjectionException e) {
			throw new RuntimeException (e);
		}
		if (resultList!=null && resultList.size()>0) result = resultList.get(0);
		return result;
	}
	
	public String setTokenStep2 (String username,String appCode) throws ResetPasswordException{
		ODocument resetRecord = getCurrentResetRecord(username);
		if (resetRecord==null) throw new ResetPasswordException("No reset record found");
		String sRandom = appCode + "%%%%" + username + "%%%%" + UUID.randomUUID();
		String sBase64Random = new String(Base64.encodeBase64(sRandom.getBytes()));
		
		resetRecord.field(ATTRIBUTES_BASE64CODE_STEP2,sBase64Random);
		resetRecord.save();
		return sBase64Random;
	}
	
	public ODocument setResetPasswordDone(String username) throws ResetPasswordException  {
		ODocument resetRecord = getCurrentResetRecord(username);
		if (resetRecord==null) throw new ResetPasswordException("No reset record found");
		
		if (resetRecord != null) {
			resetRecord.field(ATTRIBUTES_COMPLETED_DATE, new Date());
			resetRecord.save();
		}
		return resetRecord;
	}
}
