package com.baasbox.controllers;

import java.util.HashMap;
import java.util.Map;

import play.mvc.Http;


public class BodyHelper {

	/*
	 * Derived from:
	 * @see https://github.com/playframework/playframework/blob/02e05755753407bc238da6f57f551fa455d820cb/framework/src/play-java/src/main/java/play/data/Form.java#L198
	 * We deleted the querystring concatenation in the resulting hashmap
	 */
    public static Map<String,String> requestData(Http.Request request) {

        Map<String,String[]> urlFormEncoded = new HashMap<String,String[]>();
        if(request.body().asFormUrlEncoded() != null) {
            urlFormEncoded = request.body().asFormUrlEncoded();
        }

        Map<String,String[]> multipartFormData = new HashMap<String,String[]>();
        if(request.body().asMultipartFormData() != null) {
            multipartFormData = request.body().asMultipartFormData().asFormUrlEncoded();
        }

        Map<String,String> jsonData = new HashMap<String,String>();
        if(request.body().asJson() != null) {
            jsonData = play.libs.Scala.asJava(
                play.api.data.FormUtils.fromJson("", 
                    play.api.libs.json.Json.parse(
                        play.libs.Json.stringify(request.body().asJson())
                    )
                )
            );
        }

 
        Map<String,String> data = new HashMap<String,String>();

        for(String key: urlFormEncoded.keySet()) {
            String[] values = urlFormEncoded.get(key);
            if(key.endsWith("[]")) {
                String k = key.substring(0, key.length() - 2);
                for(int i=0; i<values.length; i++) {
                    data.put(k + "[" + i + "]", values[i]);
                }
            } else {
                if(values.length > 0) {
                    data.put(key, values[0]);
                }
            }
        }

        for(String key: multipartFormData.keySet()) {
            String[] values = multipartFormData.get(key);
            if(key.endsWith("[]")) {
                String k = key.substring(0, key.length() - 2);
                for(int i=0; i<values.length; i++) {
                    data.put(k + "[" + i + "]", values[i]);
                }
            } else {
                if(values.length > 0) {
                    data.put(key, values[0]);
                }
            }
        }

        for(String key: jsonData.keySet()) {
            data.put(key, jsonData.get(key));
        }

        return data;
    }
}
