/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.controllers.actions.filters;


import org.apache.commons.lang.exception.ExceptionUtils;

import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.SimpleResult;

import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.exception.ShuttingDownDBException;
import com.baasbox.exception.TransactionIsStillOpenException;
import com.baasbox.service.permissions.RouteTagger;
import com.baasbox.service.permissions.Tags;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;



/**
 * Inject the user credentials into the args argument
 * @author claudio
 */
public class ConnectToDBFilter extends Action.Simple {
 
	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		//set the current Context in the local thread to be used in the views: https://groups.google.com/d/msg/play-framework/QD3czEomKIs/LKLX24dOFKMJ
        Http.Context.current.set(ctx);

        //fixme should happen as early as possible in the action chain
        RouteTagger.attachAnnotations(ctx);


        if (Logger.isDebugEnabled()) Logger.debug("ConnectToDB for resource " + Http.Context.current().request());
		String username=(String) Http.Context.current().args.get("username");
		String password=(String)Http.Context.current().args.get("password");
		String appcode=(String)Http.Context.current().args.get("appcode");
		ODatabaseRecordTx database = null;
		F.Promise<SimpleResult> result=null;
		try{
			//close an eventually  'ghost'  connection left open in this thread
			//(this may happen in case of Promise usage)
			DbHelper.close(DbHelper.getConnection());
	        try{
	        	database=DbHelper.open(appcode,username,password);

                if(!Tags.verifyAccess(ctx)){
                    return  F.Promise.<SimpleResult>pure(forbidden("Endpoint has been disabled"));
                }
	        }catch (OSecurityAccessException e){
	        	if (Logger.isDebugEnabled()) Logger.debug(e.getMessage());
	        	return F.Promise.<SimpleResult>pure(unauthorized("User " + Http.Context.current().args.get("username") + " is not authorized to access"));
	        }catch(ShuttingDownDBException sde){
	        	String message = sde.getMessage();
	        	Logger.info(message);
	        	return F.Promise.<SimpleResult>pure(status(503,message));
	        }
			
			result = delegate.call(ctx);

			if (DbHelper.getConnection()!=null && DbHelper.isInTransaction()) throw new TransactionIsStillOpenException("Controller left an open transaction. Database will be rollbacked"); 
			
		}catch (OSecurityAccessException e){
			if (Logger.isDebugEnabled()) Logger.debug("ConnectToDB: user authenticated but a security exception against the resource has been detected: " + e.getMessage());
			result = F.Promise.<SimpleResult>pure(forbidden(e.getMessage()));
		}catch (InvalidAppCodeException e){
			if (Logger.isDebugEnabled()) Logger.debug("ConnectToDB: Invalid App Code " + e.getMessage());
			result = F.Promise.<SimpleResult>pure(unauthorized(e.getMessage()));	
		}catch (Throwable e){
			if (Logger.isDebugEnabled()) Logger.debug("ConnectToDB: an expected error has been detected: "+ ExceptionUtils.getFullStackTrace(e));
			result = F.Promise.<SimpleResult>pure(internalServerError(ExceptionUtils.getFullStackTrace(e)));	
		}finally{
			Http.Context.current.set(ctx); 
			if (DbHelper.getConnection()!=null && DbHelper.isInTransaction()) DbHelper.rollbackTransaction();
			DbHelper.close(database);
		}
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return result;
	}

}
