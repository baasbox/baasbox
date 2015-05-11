package com.baasbox.security.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class AuthenticatorService
{
    private final AuthenticatorFactory mAuthMethodFactory;
    private final Clock mClock;

    private final String issuer;
    private final String appcode;



    protected AuthenticatorService(Clock clock,String issuer,String appcode){
        this.mClock = clock;
        this.mAuthMethodFactory = AuthenticatorFactory.getDefault();
        this.issuer = issuer;
        this.appcode = appcode;
    }


    public boolean authorize(String authHeader,String encryptionKey) throws AuthException {
        JWTToken token = JWTToken.decode(authHeader, encryptionKey);
        if (isJWTTimeout(token)){
            return false; // ?? should return a reason of failure?
        }
        String authMethod = token.getMethod();
        Authenticator authenticator = mAuthMethodFactory.getAuthenticator(authMethod);
        return authenticator.authorize(token);
    }

    public Tokens login(Credentials credentials,String encryptionKey,String refreshKey) throws AuthException{
        Authenticator authenticator = mAuthMethodFactory.getAuthenticator(credentials.method);
        AuthenticationResult authenticate = authenticator.authenticate(credentials);
        if (authenticate == null){
            throw new AuthException("authenticator did not produce a result");
        }
        if (authenticate.isOk()){
            Instant now = mClock.instant();

            JWTToken token = generateJWTToken(credentials, authenticator, authenticate, now);
            String refresh = generateRefresh(refreshKey, authenticate, now);

            ObjectNode persistentState = authenticate.persistentState();

            if (persistentState != null){
                savePersistentState(persistentState, credentials.username, credentials.method);
            }
            token.encode(encryptionKey);
            return new Tokens(token.encode(encryptionKey),refresh);
        }
        return null;
    }


    private String generateRefresh(String refreshKey, AuthenticationResult authenticate, Instant now)
    {
        String refresh;
        if (authenticate.hasRefresh()){
            refresh = RefreshToken.create(now.getEpochSecond()).encode(refreshKey);
        } else {
            refresh =null;
        }
        return refresh;
    }

    private JWTToken generateJWTToken(Credentials credentials, Authenticator authenticator, AuthenticationResult authenticate, Instant now)
    {
        long expiration =authenticate.expiresIn();
        expiration = expiration == -1?expiration:now.plus(expiration, ChronoUnit.SECONDS).getEpochSecond();
        long nbf = authenticate.timeBeforeValid();
        nbf = nbf == -1?nbf:now.plus(nbf,ChronoUnit.SECONDS).getEpochSecond();

        return new JWTToken(issuer,now.getEpochSecond(),
                expiration,nbf,
                credentials.username,
                generateJTI(now),
                appcode,
                credentials.clientNonce,
                authenticator.name(),
                authenticate.claims());
    }

    private void savePersistentState(ObjectNode persistentState,String username,String method){
        //todo save persistent state
    }

    private String generateJTI(Instant now){
        String jti = UUID.randomUUID().toString();
        jti+= Long.toString(now.getEpochSecond());
        return Encoding.encodeBase64(jti);
    }

    private boolean isJWTTimeout(JWTToken token) {
        Instant now = mClock.instant().truncatedTo(ChronoUnit.SECONDS);

        Instant issuedAt = Instant.ofEpochSecond(token.getIssuedAt());
        Instant notBefore = token.getNotBefore() == -1 ? issuedAt : Instant.ofEpochSecond(token.getNotBefore());
        Instant expiresAt = token.getExpiresAt() == -1 ? null : Instant.ofEpochSecond(token.getExpiresAt());

        boolean expired = notBefore.isAfter(now);
        if (!expired && expiresAt != null){
            return expiresAt.isBefore(now);
        }
        return expired;
    }

}
