package com.baasbox.controllers.helpers;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.exception.ExceptionUtils;

import play.libs.Akka;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Results.Chunks;
import play.mvc.Results.StringChunks;
import scala.concurrent.duration.FiniteDuration;

import com.baasbox.controllers.actions.filters.WrapResponseHelper;
import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;

public abstract class  AbstractOrientChunker extends StringChunks {
    	
		private String appcode;
		private String username;
		private String password;
		private String query = "";
		private AtomicBoolean isDisconnected = new AtomicBoolean(false);
		private Context ctx;
		private QueryParams criteria;
    	
    	protected AbstractOrientChunker(){	super();	}
    	
    	public AbstractOrientChunker(String appcode, String username, String password,
    			Context ctx) {
    		super();
    		this.appcode = appcode;
    		this.username = username;
    		this.password = password;
    		this .ctx=ctx;
    		this.criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
    	}
    	
		@Override //StringChunks
		public void onReady(play.mvc.Results.Chunks.Out<String> out) {
			BaasBoxLogger.debug("***onReady start!");
			this.go(out);
		}
		
		protected abstract String prepareDocToJson(ODocument doc);

		private void go(Chunks.Out<String> out){
			final AbstractOrientChunker that = this;
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
								out.write(WrapResponseHelper.preludeOk(that.ctx));
								out.write("[");
								DbHelper.open(that.appcode,that.username,password);
								OSQLAsynchQuery<ODocument> qry = new OSQLAsynchQuery<ODocument>(that.query);
								qry.setResultListener(new OCommandResultListener() {
									boolean firstRecord=true;
									boolean more = false;
									AtomicInteger numOfRecords = new AtomicInteger(0);
									@Override
									public boolean result(Object iRecord) {
										numOfRecords.incrementAndGet();
										if (that.criteria.isPaginationEnabled()){ //"more" field!
							            	if (numOfRecords.get() > criteria.getRecordPerPage().intValue()){
							            		this.more=true;
							            		return false;
							            	} 
							            }
										String str= prepareDocToJson((ODocument) iRecord);
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
										out.write(",\"more\":" + more);
										out.write(WrapResponseHelper.endOk(that.ctx,200));
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

		public Context getHttpContext() {
			return this.ctx;
		}
		
		public void setHttpContext(Context ctx) {
			this.ctx=ctx;
			this.criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		}
    }//OrientChunker