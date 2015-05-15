package com.baasbox.security.auth;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class Tokens
{
    public final String token;
    public final Optional<String> refresh;

    private String dbPassword;

    public Tokens(String token, String refresh) {
        this.token = Objects.requireNonNull(token);
        this.refresh = Optional.ofNullable(refresh);
        this.dbPassword = null;
    }

    public static Tokens encoded(JWTToken jwt, String secret, RefreshToken rtoken, String rsecret)
    {
        return new Tokens(jwt.encode(secret),rtoken.encode(rsecret));
    }

    public static Tokens create(String token, String refreshToken)
    {
        return new Tokens(token,refreshToken);
    }

    public static Tokens createWithAttachedPassword(String token,String refreshToken,String password){
        return new Tokens(token,refreshToken);
    }

    public Tokens attachDbPassword(String password){
        this.dbPassword = password;
        return this;
    }

    public Optional<String> password(){
        return Optional.ofNullable(dbPassword);
    }
}
