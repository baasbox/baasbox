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

    public Tokens(String token, String refresh) {
        this.token = Objects.requireNonNull(token);
        this.refresh = Optional.ofNullable(refresh);
    }

    public static Tokens encoded(JWTToken jwt, String secret, RefreshToken rtoken, String rsecret)
    {
        return new Tokens(jwt.encode(secret),rtoken.encode(rsecret));
    }

    public static Tokens create(String token, String refreshToken)
    {
        return new Tokens(token,refreshToken);
    }
}
