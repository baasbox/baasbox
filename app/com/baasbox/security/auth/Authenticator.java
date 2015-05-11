package com.baasbox.security.auth;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public interface Authenticator
{

    /**
     * Name of the authenticator
     * will be set in the amr and used to find the authenticator
     * @return
     */
    String name();


    /**
     * Authorize a jwt token request
     * @param token
     * @return true if the jwt is valid for this authenticator
     */
    boolean authorize(JWTToken token);


    /**
     * Returns an authentication for credentials
     * @param credentials
     * @return
     */
    AuthenticationResult authenticate(Credentials credentials);

}
