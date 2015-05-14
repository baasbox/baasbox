package unit;

import com.baasbox.security.auth.AuthException;
import com.baasbox.security.auth.Encoding;
import com.baasbox.security.auth.JWTToken;

import com.baasbox.security.auth.RefreshToken;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.time.Clock;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class TokenTests
{

    @Test
    public void validRefreshTokenRoundTripsCorrectly(){
        RefreshToken token = makeRefreshToken();
        String encrypted = token.encode("secret");
        try {
            RefreshToken decoded = RefreshToken.decode(encrypted,"secret");
            assertEquals(token,decoded);
        } catch (AuthException e){
            fail("unexptected exception");
        }
    }

    @Test
    public void tamperedRefreshTokensFail(){
        RefreshToken token = makeRefreshToken();
        String encrypted = token.encode("secret");
        String tampered = tamperRefresh(encrypted);
        try {
            RefreshToken.decode(tampered,"secret");
            fail("should have been failed");
        } catch (AuthException e){
            assertTrue("ok",true);
        } catch (Throwable e){
            fail("unexpected failure");
        }
    }


    @Test
    public void validTokenRoundTripsCorrectly(){
        JWTToken token = makeNewToken();

        String encrypted = token.encode("secret");

        try {
            JWTToken decoded = JWTToken.decode(encrypted,"secret");

            assertEquals(token,decoded);
        } catch (AuthException e) {
            fail("unexpected exception");
        }
    }

    @Test
    public void tamperedTokensFail(){
        JWTToken token = makeNewToken();
        String encrypted = token.encode("secret");
        String tampered = tamper(encrypted);
        try {
            JWTToken.decode(tampered,"secret");
            fail("should not be decoded");
        }catch (AuthException e){
            assertTrue("ok",true);
        } catch (Throwable e){
            fail("unexpected exception "+e.getMessage());
        }
    }


    private RefreshToken makeRefreshToken() {
        String id = UUID.randomUUID().toString();
        long epochSeconds = Clock.systemUTC().instant().getEpochSecond();
        long expire = epochSeconds+100000;
        RefreshToken token =new RefreshToken(epochSeconds,expire,id);
        return token;
    }

    private JWTToken makeNewToken() {
        String jti = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        String iss = "http://com.baasbox";
        long epochSecond = Clock.systemUTC().instant().getEpochSecond();
        long expire = epochSecond+10000;
        return new JWTToken(iss,epochSecond,expire,-1,"user","com.baasbox",nonce,jti,"fake",null);
    }

    private String tamperRefresh(String original){
        int end = original.indexOf('.');
        String payloadb64 = original.substring(0, end);
        String payload = Encoding.decodeBase64(payloadb64);
        ObjectNode node = (ObjectNode) BBJson.mapper().readTreeOrMissing(payload);
        node.put("id","dsfafasfa");
        String tampered = Encoding.encodeBase64(node);
        return tampered+'.'+original.substring(end+1);
    }

    private String tamper(String original){
        int start = original.indexOf('.');
        int end = original.indexOf('.',start+1);
        String payload = original.substring(start + 1, end);
        String s = Encoding.decodeBase64(payload);
        ObjectNode node =(ObjectNode) BBJson.mapper().readTreeOrMissing(s);
        node.put("sub","random");
        String tamperedPayload = Encoding.encodeBase64(node);
        return original.substring(0,start)+'.'+tamperedPayload+'.'+original.substring(end+1);
    }

}
