package com.baasbox.security.auth;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class Credentials {
    public String username;
    public String password;
    public String method;
    public String clientNonce;

    public Credentials(String username, String password, String method, String clientNonce) {
        this.username = username;
        this.password = password;
        this.method = method;
        this.clientNonce = clientNonce;
    }
}
