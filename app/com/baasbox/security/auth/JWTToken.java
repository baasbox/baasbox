package com.baasbox.security.auth;

import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class JWTToken
{
    private static final String ALGORITHM = "alg";
    private static final String ALGORITHM_TYPE = "HS256";

    private static final String TYPE = "typ";
    private static final String JWT = "JWT";

    public static final String ISSUER = "iss";
    public static final String ISSUED_AT = "iat";
    public static final String EXPIRES_AT = "exp";
    public static final String NOT_BEFORE = "nbf";
    public static final String SUBJECT = "sub";
    public static final String AUDIENCE = "aud";
    public static final String CLIENT_ID = "nonce";
    public static final String AUTH_METHOD = "amr";
    public static final String JTI = "jti";
    public static final String EXTRA_CLAIMS="bb_user_claims";

    private final String issuer;
    private final long issuedAt;
    private long expiresAt;
    private long notBefore;
    private String subject; // a chi è indirizzato? (open id connect: identifica l'utilizzatore finale mai riassegnato)
    private  String audience;
    private String nonce; // client id
    private String amr; // authentication method reference
    private String jti; // salt
    public ObjectNode claims;

    public JWTToken(String iss, long iat, long exp, long nbf,
                     String sub, String aud, String nonce,
                     String jti, String method, ObjectNode claims)
    {
        this.issuer = iss;
        this.issuedAt = iat;
        this.expiresAt = exp;
        this.notBefore = nbf;
        this.subject = sub;
        this.audience=  aud;
        this.nonce = nonce;
        this.jti = jti;
        this.amr =method;
        this.claims = claims;

    }


    public long getNotBefore() {
        return notBefore;
    }

    public long getExpiresAt(){
        return expiresAt;
    }

    public long getIssuedAt(){
        return issuedAt;
    }

    public String getMethod() {
        return amr;
    }

    public String getSubject(){
        return subject;
    }

    public static JWTToken decode(String token,String secret) throws AuthException{
        int indexFirstDot = token.indexOf('.');
        if (indexFirstDot  == -1) {
            throw new AuthException("invalid token format");
        }
        int indexSecondDot = token.indexOf('.', indexFirstDot + 1);
        if (indexSecondDot == -1) {
            throw new AuthException("invalid token format");
        }
        String headerPart = token.substring(0, indexFirstDot);
        JsonNode headerNode = BBJson.mapper().readTreeOrMissing(Encoding.decodeBase64(headerPart));
        validateHeader(headerNode);
        String payload = token.substring(0, indexSecondDot);
        String userSignature = token.substring(indexSecondDot + 1);
        String signedPayload = Encoding.signHS256(payload, secret);
        if (!signedPayload.equals(userSignature)){
            throw new AuthException("invalid token");
        }

        String jwtPayload = payload.substring(indexFirstDot+1);
        JsonNode jwtJson = BBJson.mapper().readTreeOrMissing(Encoding.decodeBase64(jwtPayload));
        if (!jwtJson.isObject()){
            throw new AuthException("invalid token");
        }
        return jsonToJWT((ObjectNode) jwtJson);
    }


    public static JWTToken jsonToJWT(ObjectNode payload){
        String iss = payload.path(ISSUER).asText();
        long iat = payload.path(ISSUED_AT).asLong();
        long exp = payload.path(EXPIRES_AT).asLong(-1);
        long nbf = payload.path(NOT_BEFORE).asLong(-1);
        String sub = payload.path(SUBJECT).asText();
        String aud = payload.path(AUDIENCE).asText();
        String nonce = payload.path(CLIENT_ID).asText();
        String jti = payload.path(JTI).asText();
        String method = payload.path(AUTH_METHOD).asText();
        JsonNode claimsNode = payload.get(EXTRA_CLAIMS);
        ObjectNode claims;
        if (claimsNode != null && claimsNode.isObject()){
            claims = (ObjectNode)claimsNode;
        } else {
            claims = null;
        }
        return new JWTToken(iss,iat,exp,nbf,sub,aud,nonce,jti,method,claims);
    }

    private static boolean validateHeader(JsonNode header)
    {
        if (!header.isObject()) {
            return false;
        }
        JsonNode verify;
        return !((verify = header.get(ALGORITHM)) == null ||
                (!verify.isTextual()) ||
                !verify.asText().equals(ALGORITHM_TYPE))

                &&

                !((verify = header.get(TYPE)) == null ||
                        (!verify.isTextual()) ||
                        !verify.asText().equals(JWT));
    }

    public String encode(String secret) {
        ObjectNode header = createHeader();
        ObjectNode claims = encodeJson(this);
        String header64 = Encoding.encodeBase64(header);
        String claims64 = Encoding.encodeBase64(claims);
        String payload =   header64+"."+claims64;
        String signature = Encoding.signHS256(payload, secret);
        return payload+"."+signature;
    }


    private static ObjectNode encodeJson(JWTToken token){
        ObjectNode encoded = BBJson.mapper().createObjectNode();
        encoded.put(ISSUER,token.issuer);
        encoded.put(ISSUED_AT,token.issuedAt);
        if (token.expiresAt != -1) {
            encoded.put(EXPIRES_AT, token.expiresAt);
        }
        if (token.notBefore != -1){
            encoded.put(NOT_BEFORE,token.notBefore);
        }
        encoded.put(SUBJECT,token.subject);
        encoded.put(AUDIENCE,token.audience);
        encoded.put(JTI,token.jti);
        encoded.put(CLIENT_ID,token.nonce);
        encoded.put(AUTH_METHOD, token.amr);
        if (token.claims != null) {
            encoded.put(EXTRA_CLAIMS, token.claims);
        }
        return encoded;
    }

    private static ObjectNode createHeader(){
        ObjectNode header = BBJson.mapper().createObjectNode();
        header.put(ALGORITHM,ALGORITHM_TYPE);
        header.put(TYPE,JWT);
        return header;
    }

    @Override
    public String toString()
    {
        return encodeJson(this).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JWTToken token = (JWTToken) o;
        if (issuedAt != token.issuedAt) {
            return false;
        }
        if (expiresAt != token.expiresAt) {
            return false;
        }
        if (notBefore != token.notBefore) {
            return false;
        }
        if (!issuer.equals(token.issuer)) {
            return false;
        }
        if (subject != null ? !subject.equals(token.subject) : token.subject != null) {
            return false;
        }
        if (audience != null ? !audience.equals(token.audience) : token.audience != null) {
            return false;
        }
        if (nonce != null ? !nonce.equals(token.nonce) : token.nonce != null) {
            return false;
        }
        if (!amr.equals(token.amr)) {
            return false;
        }
        if (!jti.equals(token.jti)) {
            return false;
        }
        return !(claims != null ? !claims.equals(token.claims) : token.claims != null);
    }

    @Override
    public int hashCode() {
        int result = issuer.hashCode();
        result = 31 * result + (int) (issuedAt ^ (issuedAt >>> 32));
        result = 31 * result + (int) (expiresAt ^ (expiresAt >>> 32));
        result = 31 * result + (int) (notBefore ^ (notBefore >>> 32));
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (audience != null ? audience.hashCode() : 0);
        result = 31 * result + (nonce != null ? nonce.hashCode() : 0);
        result = 31 * result + amr.hashCode();
        result = 31 * result + jti.hashCode();
        return result;
    }

    public String getAudience() {
        return audience;
    }
}
