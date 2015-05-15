package com.baasbox.security.auth;

import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.Ref;
import java.util.UUID;


/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class RefreshToken {
    private final long issued;
    private final long expiration;
    private final String tokenUUID;


    public RefreshToken(long issued,long expiration,String uuid){
        this.issued = issued;
        this.expiration = expiration;
        this.tokenUUID = uuid;
    }

    public String encode(String secret){
        ObjectNode token = toJson();
        String tokenSerial = Encoding.encodeBase64(token);
        String signature = Encoding.signHS256(tokenSerial, secret);
        return tokenSerial+"."+signature;
    }

    public long getIssuedAt(){
        return issued;
    }

    public long getExpiresAt(){
        return expiration;
    }

    private ObjectNode toJson()
    {
        ObjectNode token = BBJson.mapper().createObjectNode();
        token.put("iat",issued);
        token.put("exp",expiration);
        token.put("id",tokenUUID);
        return token;
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
        JsonNode node = BBJson.mapper().readTreeOrMissing(Encoding.decodeBase64(payload));
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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RefreshToken token = (RefreshToken) o;

        if (issued != token.issued) {
            return false;
        }
        if (expiration != token.expiration) {
            return false;
        }
        return tokenUUID.equals(token.tokenUUID);

    }

    @Override
    public int hashCode()
    {
        int result = (int) (issued ^ (issued >>> 32));
        result = 31 * result + (int) (expiration ^ (expiration >>> 32));
        result = 31 * result + tokenUUID.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return toJson().toString();
    }

    public static RefreshToken create(long iat){
        return new RefreshToken(iat,-1,UUID.randomUUID().toString());
    }

    public static RefreshToken create(long epochSecond,String unique)
    {
        return new RefreshToken(epochSecond,-1, unique);
    }
}
