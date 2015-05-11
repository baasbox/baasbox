package com.baasbox.security.auth;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
class DefaultAuthenticatorFactory implements AuthenticatorFactory {
    static final DefaultAuthenticatorFactory INSTANCE = new DefaultAuthenticatorFactory();


    private ConcurrentHashMap<String,Authenticator> mAuthenticators;

    private DefaultAuthenticatorFactory(){
        mAuthenticators = new ConcurrentHashMap<>();
        //todo add default authenticators
    }

    @Override
    public Authenticator getAuthenticator(String name) throws AuthException
    {
        Authenticator authenticator = mAuthenticators.get(name);
        if (authenticator == null){
            throw new AuthException("Unknown authenticator");
        }
        return authenticator;
    }

    public void registerAuthenticator(Authenticator authenticator){
        mAuthenticators.putIfAbsent(authenticator.name(),authenticator);
    }

    public void removeAuthenticator(String name){
        mAuthenticators.remove(name);
    }
}
