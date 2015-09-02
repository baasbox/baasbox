package com.baasbox.controllers.helpers;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import play.libs.Akka;
import play.mvc.Http.Context;
import play.mvc.Results.Chunks;
import play.mvc.Results.StringChunks;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.baasbox.BBConfiguration;
import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.QueryParams;
import com.google.common.collect.ImmutableMap;
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
		
		private class DoTheDirtyJob extends UntypedActor {
		  private boolean _continue = true;
			
		  @Override
		  public void onReceive(Object message) {
			  HashMap<String,Object> params = (HashMap<String,Object>) message;
			  final Chunks.Out<String> out=(Chunks.Out<String>) params.get("out");
			  out.onDisconnected(()->{		    			
	    			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: The client is disconnected");
	    			this._continue = false; //stop the data pumping....
			  });
			  try {
			    DoTheDirtyJob thisActor = this;
			    final String appcode = (String) params.get("appcode");
			    final String username = (String) params.get("username");
			    final String password = (String) params.get("password");
			    
			    final String[] callId = (String[]) params.get("callId"); 
			    final Boolean setMoreField = (Boolean) params.get("setMoreField"); 
			    final String moreFieldValue = (String) params.get("moreFieldValue"); 
			    
			    final QueryParams criteria =  (QueryParams) params.get("criteria");
			    final String query = (String) params.get("query");
			    if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: ready to execute query: {}", query);
			    if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: opening connection");
			    
			  	DbHelper.open(appcode,username,password);
			  	
			  	String prelude = WrapResponseHelper.preludeOk (
			  			callId.length==0?null:callId[0],
			  			setMoreField,
			  			moreFieldValue);
			  	
				out.write("{" + prelude + "["); //send ASAP (bypassing the buffer) the prelude so to reply almost immediately to the client to avoid timeouts
				
				final OSQLAsynchQuery<ODocument> qry = new OSQLAsynchQuery<ODocument>(query);
				qry.setResultListener(new OCommandResultListener() {
					MyBuffer buffer = new MyBuffer(out, BBConfiguration.getChunkSize());
					boolean firstRecord=true;
					boolean more = false;
					AtomicInteger numOfRecords = new AtomicInteger(0);
					@Override
					public boolean result(Object iRecord) {
						numOfRecords.incrementAndGet();
						if (criteria.isPaginationEnabled()){ //"more" field is required (set by the controller)!
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
						return thisActor._continue;
					}
					
					@Override
					public void end() {
						buffer.write("]");
						if (criteria.isPaginationEnabled()) buffer.write(",\"more\":" + more);
						buffer.write(WrapResponseHelper.endOk(200) + "}");
						buffer.close();
						if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: Finished to stream chunked response ({} records sent)",numOfRecords.get());
					}
				}); //setResultListener
				OCommandRequest command = DbHelper.getConnection().command(qry);
				command.execute();
	    	}catch (Exception e) {
	    		String exceptionMessage = ExceptionUtils.getFullStackTrace(e);
	    		BaasBoxLogger.error(exceptionMessage);
	    		if (out!=null){	
	    			out.write("\nSorry, an error has occured!\n");
	    			out.write(exceptionMessage);
					out.close();
	    		}
			}finally{
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CHUNKED: closing connection");
				DbHelper.close(DbHelper.getConnection());
			}
		  }//onReceive
		} //actor DoTheDirtyJob
    	
		private String appcode;
		private String username;
		private String password;
		private String query = "";
		private QueryParams criteria;
		private String moreFieldValue;
		private boolean setMoreField;
		private String[] callId;
    	
    	protected AbstractOrientChunker(){	super();	}
    	
    	public AbstractOrientChunker(String appcode, String username, String password,
    			Context ctx) {
    		super();
    		this.appcode = appcode;
    		this.username = username;
    		this.password = password;
    		this.callId = ctx.request().queryString().get("call_id");
    		this.setMoreField = !StringUtils.isEmpty(ctx.response().getHeaders().get("X-BB-MORE"));
    		this.moreFieldValue = ctx.response().getHeaders().get("X-BB-MORE");
    		
    		this.criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
    	}
    	
		@Override //StringChunks
		public void onReady(play.mvc.Results.Chunks.Out<String> out) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Start to stream chunked response");
			this.go(out);
		}
		
		private void go(Chunks.Out<String> out){
    		//prepare parameters to pass to the worker
			HashMap<String,Object> params = new HashMap<String, Object>();
			{
				params.put("appcode", this.appcode);
				params.put("username", this.username);
				params.put("password", this.password);
	
				params.put("callId", this.callId);
				params.put("setMoreField", this.setMoreField);
				params.put("moreFieldValue", this.moreFieldValue);
				
				params.put("criteria", this.criteria);
				params.put("query", this.query);
				
				params.put("out", out);
			}
			
			ActorRef actorRef = Akka.system().actorOf(Props.create(DoTheDirtyJob.class, this));
			Akka.system().scheduler().scheduleOnce(
    				Duration.Zero(), //runs as soon as possible
    				actorRef,
    			    params, 
    			    Akka.system().dispatcher(),null
    			    );
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

		
		protected abstract String prepareDocToJson(ODocument doc);

    }//OrientChunker