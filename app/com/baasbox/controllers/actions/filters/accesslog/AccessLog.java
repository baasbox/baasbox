package com.baasbox.controllers.actions.filters.accesslog;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;

import org.apache.http.HttpStatus;
import org.codehaus.jackson.node.ObjectNode;

import play.GlobalSettings;
import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.HeaderNames;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import scala.collection.JavaConversions;

public class AccessLog extends GlobalSettings {

	public AccessLog() {
		System.out.println("AlogJGlobal instantiated");
	}

	protected LogEntry toLogEntry(long tstmp, Request request, long duration, Result result) {
		if (request == null || !(result.getWrappedResult() instanceof play.api.mvc.PlainResult)) return null;

		play.api.mvc.PlainResult r = (play.api.mvc.PlainResult)result.getWrappedResult();

		long requestSize;
		try {
			requestSize = Long.parseLong(request.getHeader(HeaderNames.CONTENT_LENGTH));
		} catch (Exception e) {
			requestSize = -1L;
		}

		long responseSize;
		try {
			responseSize = Long.parseLong(r.header().headers().get(HeaderNames.CONTENT_LENGTH).get());
		} catch (Exception e) {
			responseSize = -1L;
		}

		Collection<Map.Entry<String,String>> reqhdrs = new ArrayList<Map.Entry<String,String>>();
		for(Map.Entry<String,String[]> e: request.headers().entrySet()) {
			if (e.getValue() == null) {
				reqhdrs.add(new AbstractMap.SimpleEntry<String,String>(e.getKey(),null));
			} else if (e.getValue().length <= 0) {
				reqhdrs.add(new AbstractMap.SimpleEntry<String,String>(e.getKey(),null));
			} else if (e.getValue().length == 1) {
				reqhdrs.add(new AbstractMap.SimpleEntry<String,String>(e.getKey(),e.getValue()[0]));
			} else {
				for(String s: e.getValue()) {
					reqhdrs.add(new AbstractMap.SimpleEntry<String,String>(e.getKey(),s));
				}
			}
		}
		Collection<Map.Entry<String,String>> reshdrs = JavaConversions.mapAsJavaMap(r.header().headers()).entrySet();

		return new LogEntry(
				tstmp, request.method(), request.uri(), "HTTP/1.1", request.remoteAddress(), requestSize, reqhdrs, 
				duration, r.header().status(), responseSize, reshdrs);
	}

	protected LogEntry toLogEntry(long tstmp, String uri, long duration, int status) {
		return new LogEntry(tstmp, "?", uri, "?", duration, status,-1);
	}
	
	protected LogEntry toLogEntry(long tstmp, String uri, long duration, int status,long responseSize) {
	 	return new LogEntry(tstmp, "?", uri, "?", duration, status,responseSize);
	}



	@Override
	public Action<?> onRequest(final Request request, Method actionMethod) {
		final long beg = System.currentTimeMillis();

		// eventhough superclass GlobalSettings 's onRequest method is simple today, 
		// wrapping it so alog can be future proof
		final Action superAction = super.onRequest(request, actionMethod);

		Action<?> alogAction = new Action.Simple() {

			public Result call(Context ctx) throws Throwable {
				if (superAction.delegate == null) {
					superAction.delegate = delegate;
				}
				Result result = superAction.call(ctx);
				long end = System.currentTimeMillis();
				LogEntry alog = toLogEntry(beg, request, end-beg, result);
				AccessLogUtils.getInstance().spool(alog);
				return result;
			}
		};  
		return alogAction;
	}



	@Override
	public Result onBadRequest(RequestHeader requestHeader, String error) {
		return onBadRequest(requestHeader, error,Http.Status.BAD_REQUEST,error.length()*8);
	}

	private Result onBadRequest(RequestHeader requestHeader, String error,int code,long responseSize){
		long now = System.currentTimeMillis();
		LogEntry alog = toLogEntry(now, requestHeader.uri(), 0,code,responseSize);
		
		AccessLogUtils.getInstance().spool(alog);
		return super.onBadRequest(requestHeader, error);
	}



	public Result onBadRequest(RequestHeader requestHeader,ObjectNode result, String error) {
		int errorcode = Http.Status.BAD_REQUEST;
		if(result.get("http_code")!=null){
			errorcode = result.get("http_code").asInt();
		}
		long size = result.asText().length()*8;
		onBadRequest(requestHeader, error,errorcode,size);
		return internalServerError(result);
	}



	@Override
	public Result onError(RequestHeader requestHeader,Throwable err) {
		// don't have access to Request object, even Context.current().request() returns null
		return onError(Http.Status.INTERNAL_SERVER_ERROR, requestHeader,err);
	}
	
	public Result onError(int statusCode,RequestHeader requestHeader,Throwable err) {
		// don't have access to Request object, even Context.current().request() returns null
		long now = System.currentTimeMillis();
		LogEntry alog = toLogEntry(now, requestHeader.uri(), 0, statusCode);
		AccessLogUtils.getInstance().spool(alog);
		return super.onError(requestHeader,err);
	}


	@Override
	public Result onHandlerNotFound(RequestHeader requestHeader) {
		// don't have access to Request object, even Context.current().request() returns null
		long now = System.currentTimeMillis();
		LogEntry alog = toLogEntry(now, requestHeader.uri(), 0, Http.Status.NOT_FOUND); 
		AccessLogUtils.getInstance().spool(alog);
		return super.onHandlerNotFound(requestHeader);
	}

	public Result onHandlerNotFound(RequestHeader requestHeader,ObjectNode result) {
		onHandlerNotFound(requestHeader);
		return notFound(result);
	}
}
