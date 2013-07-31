package com.baasbox.controllers.actions.filters.accesslog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

public class LogEntry {
  
  public enum Field {
    Timestamp,
    Method,
    URI,
    ProtoVer,
    RemoteAddr,
    RequestSize,
    RequestBody,
    Duration,
    Status,
    ResponseSize,
    ResponseBody,
    AllRequestHeaders,
    AllResponseHeaders
  }
  
  private final ImmutableMap<Object, Object> _fields;
  private final ListMultimap<String,String> _requestHeaders;
  private final ListMultimap<String,String> _responseHeaders;

  public LogEntry(long tstmp, String method, String uri, String remoteAddr, long duration, int status,long responseSize) {

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(tstmp);
    
    Builder<Object, Object> builder = ImmutableMap.builder().
        put(Field.Timestamp.name(), cal).
        put(Field.Method.name(), method).
        put(Field.URI.name(), uri).
        put(Field.RemoteAddr.name(), remoteAddr).
        put(Field.RequestSize.name(), -1L).
        put(Field.Duration.name(), duration).
        put(Field.ResponseSize.name(), responseSize).
        put(Field.Status.name(), status);
    
    _requestHeaders = null;
    _responseHeaders = null;
    
    _fields = builder.build();
  }
  
  public LogEntry(
      long tstmp, String method, String uri, String protocolVersion, String remoteAddr, long requestSize, Collection<Map.Entry<String,String>> requestHeaders, 
      long duration, int status, long responseSize, Collection<Map.Entry<String,String>> responseHeaders
  ) {
    
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(tstmp);
    
    ImmutableMap.Builder<Object,Object> builder = ImmutableMap.builder().
        put(Field.Timestamp.name(), cal).
        put(Field.Method.name(), method).
        put(Field.URI.name(), uri).
        put(Field.ProtoVer.name(), protocolVersion).
        put(Field.RemoteAddr.name(), remoteAddr).
        put(Field.RequestSize.name(), requestSize).
        put(Field.Duration.name(), duration).
        put(Field.Status.name(), status).
        put(Field.ResponseSize.name(), responseSize);
    
    Supplier<ArrayList<String>> initList = new Supplier<ArrayList<String>>() { 
      public ArrayList<String> get() { 
        return new ArrayList<String>(); 
      } 
    };
    _requestHeaders = Multimaps.newListMultimap(new LinkedHashMap<String,Collection<String>>(), initList); 
    for (Map.Entry<String,String> e : requestHeaders) { _requestHeaders.put(e.getKey().toUpperCase(), e.getValue()); }
    
    _responseHeaders = Multimaps.newListMultimap(new LinkedHashMap<String,Collection<String>>(), initList);
    for (Map.Entry<String,String> e : responseHeaders) { _responseHeaders.put(e.getKey().toUpperCase(), e.getValue()); }
    
    _fields = builder.build();
  }

  public String formatMultiVal(List<String> values) {

    if (values == null) return null;

    switch (values.size()) {
      case 0: return "";
      case 1: return values.get(0);
      default: return values.toString();
    }

  }
  public Object[] pick(String[] fieldNames) {
    Object[] objs = new Object[fieldNames.length];
    
    for (int i = 0; i < fieldNames.length; i++) {
      String fname = fieldNames[i];
      if (fname == null || fname.length() <= 0) {
        objs[i] = null;
      }else if (Field.AllRequestHeaders.name().equals(fname) && _requestHeaders != null) {
        objs[i] = _requestHeaders.toString();
      } else if (Field.AllResponseHeaders.name().equals(fname) && _responseHeaders != null) {
        objs[i] = _responseHeaders.toString();
      } else if (fname.startsWith("@") && _requestHeaders != null) {
        objs[i] = formatMultiVal(_requestHeaders.get(fname.substring(1)));
      } else if (fname.startsWith("!") && _responseHeaders != null) {
        objs[i] = formatMultiVal(_responseHeaders.get(fname.substring(1)));
      } else {
        objs[i] = _fields.containsKey(fname) ? _fields.get(fname) : "";
      }
    }
    return objs;
  }
}
