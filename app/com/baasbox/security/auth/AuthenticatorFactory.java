package com.baasbox.security.auth;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public interface AuthenticatorFactory
{
    Authenticator getAuthenticator(String name) throws AuthException;

    static AuthenticatorFactory getDefault(){
        return DefaultAuthenticatorFactory.INSTANCE;
    }
}
