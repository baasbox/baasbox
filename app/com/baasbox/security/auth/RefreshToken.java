package com.baasbox.security.auth;

import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class RefreshToken {
    private long issued;
    private long expiration;
    private String tokenUUID;

    public RefreshToken(long issued,long expiration,String uuid){
        this.issued = issued;
        this.expiration = expiration;
        this.tokenUUID = uuid;
    }

    public String encode(String secret){
        ObjectNode token = Json.mapper().createObjectNode();
        token.put("iat",issued);
        token.put("exp",expiration);
        token.put("id",tokenUUID);
        String tokenSerial = Encoding.encodeBase64(token);
        String signature = Encoding.signHS256(tokenSerial, secret);
        return tokenSerial+"."+signature;
    }

    public static RefreshToken decode(String refresh,String secret) throws AuthException{
        int i = refresh.indexOf('.');
        if (i==-1){
            throw new AuthException("Invalid token");
        }
        String payload = refresh.substring(0,i);
        String signature = refresh.substring(i+1);
        String verify = Encoding.signHS256(payload, secret);
        if (!verify.equals(signature)){
            throw new AuthException("Invalid token");
        }
        JsonNode node = Json.mapper().readTreeOrMissing(Encoding.decodeBase64(payload));
        return decodeRefresh(node);
    }

    private static RefreshToken decodeRefresh(JsonNode node) throws AuthException
    {
        if (node.isObject()){
            ObjectNode json = (ObjectNode)node;
            return new RefreshToken(json.get("iat").asLong(),json.get("exp").asLong(),json.get("id").asText());
        }
        throw new AuthException("invalid token");
    }


}
