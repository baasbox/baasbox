package com.baasbox.exception;

/**
 * Created by eto on 25/09/14.
 */
public class AlreadyFriendsException extends Exception {
    public AlreadyFriendsException() {
    }

    public AlreadyFriendsException(String message) {
        super(message);
    }

    public AlreadyFriendsException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyFriendsException(Throwable cause) {
        super(cause);
    }
}
