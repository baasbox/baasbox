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
}
