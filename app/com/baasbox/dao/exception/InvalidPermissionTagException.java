package com.baasbox.dao.exception;

/**
 *
 * Created by Andrea Tortorella on 08/04/14.
 */
public class InvalidPermissionTagException extends Exception {
    public InvalidPermissionTagException() {
    }

    public InvalidPermissionTagException(String message) {
        super(message);
    }

    public InvalidPermissionTagException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPermissionTagException(Throwable cause) {
        super(cause);
    }

    public InvalidPermissionTagException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
