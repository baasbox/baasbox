package com.baasbox.security.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class Encoding
{
    static final String JALGORITHM = "HmacSHA256";
    static final Charset UTF = Charset.forName("UTF-8");

    private Encoding(){}

    public static String decodeBase64(String part){
        byte[] decode = Base64.getUrlDecoder().decode(part);
        return new String(decode, UTF);
    }

    public static String encodeBase64(ObjectNode node){
        String json = node.toString();
        return encodeBase64(json);
    }

    static String encodeBase64(String json){
        byte[] bytes = json.getBytes(UTF);
        return encodeBase64(bytes);
    }

    public static String encodeBase64(byte[] bytes){
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String signHS256(String payload, String secret){
        try {
            SecretKey sk = new SecretKeySpec(secret.getBytes(), JALGORITHM);
            Mac mac = Mac.getInstance(JALGORITHM);
            mac.init(sk);
            byte[] output = mac.doFinal(payload.getBytes(UTF));
            return encodeBase64(output);
        } catch (InvalidKeyException | NoSuchAlgorithmException e){
            throw new AssertionError("Cryprography error: ",e);
        }
    }
}
