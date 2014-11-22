package com.baasbox.exception;

/**
 * Created by eto on 25/09/14.
 */
public class UserToFollowNotExistsException extends UserNotFoundException {
    public UserToFollowNotExistsException() {
    }

    public UserToFollowNotExistsException(String arg0) {
        super(arg0);
    }

    public UserToFollowNotExistsException(Throwable arg0) {
        super(arg0);
    }

    public UserToFollowNotExistsException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
