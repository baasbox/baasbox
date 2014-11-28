package com.baasbox.service.webservices;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;
import play.libs.WS;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by eto on 08/10/14.
 */
public class HttpClientService {



    public static WS.Response callSync(String url,String method,Map<String,List<String>> params,Map<String,List<String>> headers,Object body) throws Exception{
       try {
           return call(url, method, params, headers, (Object) body).get(3000, TimeUnit.MILLISECONDS);
       } catch (Exception e){
           throw e;
       }
    }

    public static F.Promise<WS.Response> call(String url,String method,Map<String,List<String>> params,Map<String,List<String>> headers,Object body){
        WS.WSRequestHolder req = WS.url(url);
        appendParams(req,headers, WS.WSRequestHolder::setHeader);
        appendParams(req,params,WS.WSRequestHolder::setQueryParameter);
        switch (method.toLowerCase()){
            case "get": return req.get();
            case "delete": return req.delete();
            case "post":
                return body instanceof JsonNode?req.put((JsonNode)body):req.put((String)body);
            case "put":
                return body instanceof JsonNode?req.put((JsonNode)body):req.put((String)body);
            default:
                return req.execute(method);
        }
    }

    private interface Append{
        public void append(WS.WSRequestHolder req,String v,String p);
    }

    private static void appendParams(WS.WSRequestHolder ws,Map<String,List<String>> params,Append appender){
        if (params!=null){
            params.forEach((k,vals)->{
                if (vals!=null){
                    vals.forEach((val)->{
                        if (val!=null){
                            appender.append(ws, k, val);
                        }
                    });
                }
            });
        }
    }
}
