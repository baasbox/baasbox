package com.baasbox.security.auth;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class AuthException extends Exception
{
    public AuthException()
    {
    }

    public AuthException(String message)
    {
        super(message);
    }

    public AuthException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public AuthException(Throwable cause)
    {
        super(cause);
    }
}
