package com.baasbox.controllers.actions.filters;

import play.mvc.Http;

/**
 * Created by Andrea Tortorella on 5/12/15.
 */
public class InvalidAccessMethod implements IAccessMethod
{
    public static final IAccessMethod INSTANCE = new InvalidAccessMethod();

    @Override
    public boolean setCredential(Http.Context ctx)
    {
        return false;
    }

    @Override
    public boolean isValid()
    {
        return false;
    }
}
