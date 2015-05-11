package com.baasbox.security.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Andrea Tortorella on 5/11/15.
 */
public class AuthenticationResult
{
    private final long mExpiresIn;
    private final long mTimeBefore;
    private final ObjectNode claims;
    private final ObjectNode persistentState;
    private final boolean mOk;
    private final boolean mRefresh;

    public AuthenticationResult(long expiresIn, long timeBefore, ObjectNode claims, ObjectNode persistentState, boolean ok, boolean refresh)
    {
        mExpiresIn = expiresIn;
        mTimeBefore = timeBefore;
        this.claims = claims;
        this.persistentState = persistentState;
        mOk = ok;
        mRefresh = refresh;
    }


    public long expiresIn()
    {
        return mExpiresIn;
    }

    public long timeBeforeValid()
    {
        return mTimeBefore;
    }

    public ObjectNode claims()
    {
        return claims;
    }

    public ObjectNode persistentState()
    {
        return persistentState;
    }

    public boolean isOk()
    {
        return mOk;
    }

    public boolean hasRefresh()
    {
        return mRefresh;
    }

    public static AuthenticationResult fail(){
        return new Builder(false).setExpiresIn(-1).setTimeBefore(-1).addClaims(null).setPersistentState(null).setRefresh(false).build();
    }

    public static AuthenticationResult.Builder ok(){
        return new Builder(true);
    }

    public static class Builder {

        private long mExpiresIn = -1;
        private long mTimeBefore = -1;
        private ObjectNode mClaims = null;
        private ObjectNode mPersistentState = null;
        private boolean mOk;
        private boolean mRefresh = true;

        private Builder(boolean ok){
            this.mOk = ok;
        }

        public Builder setExpiresIn(long expiresIn)
        {
            mExpiresIn = expiresIn;
            return this;
        }

        public Builder setTimeBefore(long timeBefore)
        {
            mTimeBefore = timeBefore;
            return this;
        }

        public Builder addClaims(ObjectNode claims)
        {
            mClaims = claims;
            return this;
        }

        public Builder setPersistentState(ObjectNode persistentState)
        {
            mPersistentState = persistentState;
            return this;
        }


        public Builder setRefresh(boolean refresh)
        {
            mRefresh = refresh;
            return this;
        }

        public AuthenticationResult build()
        {
            return new AuthenticationResult(mExpiresIn, mTimeBefore, mClaims, mPersistentState, mOk, mRefresh);
        }
    }
}
