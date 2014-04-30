package com.baasbox.dao.exception;

/**
 * Created by Andrea Tortorella on 08/04/14.
 */
public class PermissionTagAlreadyExistsException extends Exception {
    public PermissionTagAlreadyExistsException() {
    }

    public PermissionTagAlreadyExistsException(String message) {
        super(message);
    }

    public PermissionTagAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermissionTagAlreadyExistsException(Throwable cause) {
        super(cause);
    }

}
