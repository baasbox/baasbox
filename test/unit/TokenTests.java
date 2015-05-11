package unit;

import com.baasbox.security.auth.AuthException;
import com.baasbox.security.auth.Encoding;
import com.baasbox.security.auth.JWTToken;

import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import play.Logger;

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

    private JWTToken makeNewToken()
    {
        String jti = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        String iss = "http://com.baasbox";
        long epochSecond = Clock.systemUTC().instant().getEpochSecond();
        long expire = epochSecond+10000;
        return new JWTToken(iss,epochSecond,expire,-1,"user","com.baasbox",nonce,jti,"fake",null);
    }


    private String tamper(String original){
        int start = original.indexOf('.');
        int end = original.indexOf('.',start+1);
        String payload = original.substring(start + 1, end);
        String s = Encoding.decodeBase64(payload);
        ObjectNode node =(ObjectNode) Json.mapper().readTreeOrMissing(s);
        node.put(SUBJECT,"random");
        String tamperedPayload = Encoding.encodeBase64(node);
        return original.substring(0,start)+'.'+tamperedPayload+'.'+original.substring(end+1);
    }

}
