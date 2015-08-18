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

import com.baasbox.BBConfiguration;
import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;

public abstract class  AbstractOrientChunker extends StringChunks {
	
		private class MyBuffer{
		    private StringBuilder internalBuffer;
		    private Out<String> out;

		    public MyBuffer(Out<String> out, int bufferSize) {
		        this.out = out;
		        this.internalBuffer = new StringBuilder(bufferSize);
		    }

		    public void write(String data) {
		        if ((internalBuffer.length() + data.length()) > internalBuffer.capacity()) {
		        	out.write(internalBuffer.toString());
		        	internalBuffer.setLength(0);
		        }
		        internalBuffer.append(data);
		    }

		    public void close() {
		        if (internalBuffer.length() > 0){
		            out.write(internalBuffer.toString());
		        }
		        out.close();
		        out.write("STOP!"); //force the disconnection
		    }
		}
    	
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
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Start to stream chunked response");
			this.go(out);
		}
		
		protected abstract String prepareDocToJson(ODocument doc);

		private void go(Chunks.Out<String> out){
			final AbstractOrientChunker that = this;
			out.onDisconnected(()->{
    			that.isDisconnected.getAndSet(true);
    			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: Aborting chunked response due client disconnection");
    		});
    		Akka.system().scheduler().scheduleOnce(
    			    new FiniteDuration(0, TimeUnit.MILLISECONDS), //runs as soon as possible
    			    new Runnable () {
						@Override
						public void run() {
							try {
								//that.request.headers();
								out.write("{" + WrapResponseHelper.preludeOk(that.ctx) + "[");
								DbHelper.open(that.appcode,that.username,password);
								OSQLAsynchQuery<ODocument> qry = new OSQLAsynchQuery<ODocument>(that.query);
								qry.setResultListener(new OCommandResultListener() {
									MyBuffer buffer = new MyBuffer(out, BBConfiguration.getChunkSize());
									boolean firstRecord=true;
									boolean more = false;
									AtomicInteger numOfRecords = new AtomicInteger(0);
									@Override
									public boolean result(Object iRecord) {
										numOfRecords.incrementAndGet();
										if (that.criteria.isPaginationEnabled()){ //"more" field is required (set by the controller)!
							            	if (numOfRecords.get() > criteria.getRecordPerPage().intValue()){
							            		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: Stop streaming chunked response due pagination");
							            		this.more=true;
							            		return false;
							            	} 
							            }
										String str= prepareDocToJson((ODocument) iRecord);
										if (!firstRecord){
											buffer.write(",");
										}else{
											if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: First record written into the buffer");
											firstRecord=false;
										}
										buffer.write(str);
										return !that.isDisconnected.get();
									}
									
									@Override
									public void end() {
										buffer.write("]");
										if (that.criteria.isPaginationEnabled()) buffer.write(",\"more\":" + more);
										buffer.write(WrapResponseHelper.endOk(that.ctx,200) + "}");
										buffer.close();
										if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: Finished to stream chunked response ({} records sent)",numOfRecords.get());
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