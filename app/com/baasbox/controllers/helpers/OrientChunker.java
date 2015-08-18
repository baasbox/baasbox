package com.baasbox.controllers.helpers;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.exception.ExceptionUtils;

import play.libs.Akka;
import play.mvc.Http.Request;
import play.mvc.Results.Chunks;
import play.mvc.Results.StringChunks;
import scala.concurrent.duration.FiniteDuration;

import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;

public class OrientChunker extends StringChunks {
    	
		private String appcode;
		private String username;
		private String password;
		private String query;
		private AtomicBoolean isDisconnected = new AtomicBoolean(false);
		private Request request;
    	
    	public OrientChunker(){	super();	}
    	
		@Override //StringChunks
		public void onReady(play.mvc.Results.Chunks.Out<String> out) {
			BaasBoxLogger.debug("***onReady start!");
			this.go(out);
		}
		

		private void go(Chunks.Out<String> out){
			final OrientChunker that = this;
			out.onDisconnected(()->{
    			this.isDisconnected.getAndSet(true);
    		});
    		Akka.system().scheduler().scheduleOnce(
    			    new FiniteDuration(0, TimeUnit.MILLISECONDS), //runs as soon as possible
    			    new Runnable () {
						@Override
						public void run() {
							try {
								//that.request.headers();
								out.write("[");
								DbHelper.open(that.appcode,that.username,password);
								OSQLAsynchQuery<ODocument> qry = new OSQLAsynchQuery<ODocument>(that.query);
								qry.setResultListener(new OCommandResultListener() {
									boolean firstRecord=true;
									@Override
									public boolean result(Object iRecord) {
										String str=((ODocument)iRecord).toJSON(); //TODO: prepareDocToJson()
										BaasBoxLogger.debug("***Scrivo... " + str.length() + "bytes");
										if (!firstRecord){
											out.write(",");
										}else{
											firstRecord=false;
										}
										out.write(str);
										return !that.isDisconnected.get();
									}
									
									@Override
									public void end() {
										BaasBoxLogger.debug("***Chiudo... ");
										out.write("]");
										out.close();
									}
								});
								OCommandRequest command = DbHelper.getConnection().command(qry);
								command.execute();
	    			    	} catch (Exception e) {
	    			    		BaasBoxLogger.error(ExceptionUtils.getFullStackTrace(e));
								throw new RuntimeException(e);
							}finally{
								DbHelper.close(DbHelper.getConnection());
							}
						}
					}, Akka.system().dispatcher());
    	}
		
		public void setAppCode(String appcode) {
			this.appcode = appcode;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public void setPassword(String password) {
			this.password=password;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public void setRequestHeader(Request request) {
			this.request=request;
		}
    }//OrientChunker