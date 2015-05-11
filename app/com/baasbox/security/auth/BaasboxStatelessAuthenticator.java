package com.baasbox.security.auth;


/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class BaasboxStatelessAuthenticator implements Authenticator
{
    @Override
    public String name()
    {
        return "baasbox";
    }

    @Override
    public boolean authorize(JWTToken token)
    {
        return true;
    }

    @Override
    public AuthenticationResult authenticate(Credentials credentials)
    {

        return null;
    }
}
