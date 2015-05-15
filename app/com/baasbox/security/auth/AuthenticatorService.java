package com.baasbox.security.auth;

import com.baasbox.BBConfiguration;
import com.baasbox.db.DbHelper;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import play.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class AuthenticatorService
{

    //fixme fixed fields need to be read from somewhere
    private static final Duration DURATION = Duration.of(15, ChronoUnit.MINUTES);
    private static final String TEMP_SECRET = "secret";
    private static final String TEMP_RSECRET = "rsecret";

    public static final String AUTH_METHOD ="baasbox";

    private static final AuthenticatorService INSTANCE = new AuthenticatorService(Clock.systemUTC(),
            "baasbox.com",AUTH_METHOD);

    private final Clock mClock;
    private final String mIssuer;
    private final String mAuthMethod;

    public AuthenticatorService(Clock clock,String issuer,String authMethod) {
        this.mClock = clock;
        this.mIssuer = issuer;
        this.mAuthMethod = authMethod;

    }

    public static AuthenticatorService getInstance(){
        return INSTANCE;
    }


    public Tokens signup(Credentials credentials,JsonNode nonAppAttributes,JsonNode privateAttributes,JsonNode friendsAttributes,JsonNode appAttributes) throws Exception{
        if (Strings.isNullOrEmpty(credentials.username)) throw new IllegalArgumentException("Missing username");
        if (Strings.isNullOrEmpty(credentials.password)) throw new IllegalArgumentException("Missing password");
        try {
            DbHelper.requestTransaction();
            CredentialsDao cdao = CredentialsDao.getInstance();
            String internalPassword = genNonRepetableRandom();
            // we have credentials
            ODocument credentialsRecord =cdao.createUserCredentials(credentials.username, credentials.password, internalPassword);
            // we have the user
            ODocument user = UserService.signUp(credentials.username, internalPassword, new Date(), nonAppAttributes, privateAttributes, friendsAttributes, appAttributes, false);

            // tokens are generated
            Tokens tokens = generateTokenPair(credentials.username, credentials.clientNonce, TEMP_SECRET, DURATION, true, TEMP_RSECRET);

            // refrehs token is stored in the index
            TokenDao.getInstance().storeToken(tokens.refresh.get(), credentials.username);
            DbHelper.commitTransaction();

            return tokens;
        } catch (Exception e){
            DbHelper.rollbackTransaction();
            throw e;
        }
    }





    public Optional<JWTToken> validateJWTToken(String token,String secret){
        try {
            Instant now = mClock.instant().truncatedTo(ChronoUnit.SECONDS);

            JWTToken jwt = JWTToken.decode(token, secret);
            if (AUTH_METHOD.equals(jwt.getMethod())
                && isInTimeFrame(jwt,now)){
                    return Optional.of(jwt);
            }
        } catch (AuthException e) {
            if (Logger.isDebugEnabled())Logger.debug("Invalid token");
        }
        return Optional.empty();
    }

    public String loadUserFromRefresh(RefreshToken token){
        return null;
    }

    public Optional<RefreshToken> validateRefreshToken(String token,String rsecret){
            Instant now = mClock.instant().truncatedTo(ChronoUnit.SECONDS);
            try {
                RefreshToken refresh = RefreshToken.decode(token,rsecret);
                if (isInTimeFrame(refresh,now)){
                    return Optional.of(refresh);
                }
            } catch (AuthException e) {
                if (Logger.isDebugEnabled())Logger.debug("Invalid token");
            }
        return Optional.empty();
    }

    private boolean isInTimeFrame(RefreshToken token,Instant now){
        Instant issued = Instant.ofEpochSecond(token.getIssuedAt());
        if (now.isBefore(issued)){
            return false;
        }
        if (token.getExpiresAt()==-1){
            return true;
        }
        Instant end = Instant.ofEpochSecond(token.getExpiresAt());
        return !end.isBefore(now);
    }


    private boolean isInTimeFrame(JWTToken token,Instant now){
        Instant iat=Instant.ofEpochSecond(token.getIssuedAt());
        Instant start = token.getNotBefore()==-1?iat:Instant.ofEpochSecond(token.getNotBefore());
        if (now.isBefore(start)){
            return false;
        }
        if (token.getExpiresAt()==-1){
            return true;
        }
        Instant end = Instant.ofEpochSecond(token.getExpiresAt());
        return !end.isBefore(now);
    }




    public Tokens generateTokenPair(String user,
                                    String clientNonce,
                                    String secret, Duration duration,
                                    boolean refresh,
                                    String rsecret){
        Instant now = mClock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expires = duration == null?null:now.plus(duration);
        String jti = genNonRepetableRandom();
        JWTToken token = new JWTToken(mIssuer,
                                      now.getEpochSecond(),
                                      expires==null?-1L:expires.getEpochSecond(),
                                      -1,user,
                                      BBConfiguration.getAPPCODE(),
                                      clientNonce,
                                      jti,
                                      mAuthMethod,
                                      null);

        String encodedRefreshToken;

        if (refresh) {
            RefreshToken rtoken = RefreshToken.create(now.getEpochSecond(),genNonRepetableRandom());
            encodedRefreshToken = rtoken.encode(rsecret);
            TokenDao dao = TokenDao.getInstance();
            dao.storeToken(encodedRefreshToken,user);
        } else {
            encodedRefreshToken = null;
        }

        return Tokens.create(token.encode(secret),encodedRefreshToken);
    }

    private String genNonRepetableRandom(){
        UUID uuid = UUID.randomUUID();
        Instant now = mClock.instant();
        return Encoding.encodeBase64(uuid.toString()+now.toString());
    }
}
