package com.baasbox.controllers.actions.filters;

import com.baasbox.security.SessionKeys;
import com.baasbox.security.auth.AuthenticatorService;
import com.baasbox.security.auth.JWTToken;
import com.baasbox.service.logging.BaasBoxLogger;
import com.google.common.base.Strings;
import play.mvc.Http;

import java.util.Optional;

/**
 * Created by Andrea Tortorella on 5/15/15.
 */
public class JWTAccessMethod implements IAccessMethod
{
    public static final IAccessMethod INSTANCE = new JWTAccessMethod();
    @Override
    public boolean isAnonymous()
    {
        return false;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public boolean setCredential(Http.Context ctx)
    {
        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("JWTTokenAccess");
        String token= ctx.request().getHeader(SessionKeys.TOKEN.toString());
        if (Strings.isNullOrEmpty(token)) token= ctx.request().getQueryString(SessionKeys.TOKEN.toString());
        if (Strings.isNullOrEmpty(token)){
            String auth = ctx.request().getHeader("authorization");
            if (!Strings.isNullOrEmpty(auth) &&auth.startsWith("Bearer ")){
                token = auth.substring("Bearer ".length());
            }
        }
        final String serializedToken = token;
        Optional<JWTToken> jwt = AuthenticatorService.getInstance().validateJWTToken(token);
        jwt.ifPresent(t->{
            ctx.args.put("token",serializedToken);
            ctx.args.put("jwt",t);
            ctx.args.put("username",t.getSubject());
            ctx.args.put("appcode",t.getAudience());
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("username: " + t.getSubject());
            //if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("password: <hidden>" );
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("appcode: " + t.getAudience());
            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("token: " + serializedToken);
        });
        return jwt.isPresent();
    }
}
