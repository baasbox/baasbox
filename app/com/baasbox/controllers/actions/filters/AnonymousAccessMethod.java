package com.baasbox.controllers.actions.filters;

import com.baasbox.BBConfiguration;
import com.google.common.base.Strings;
import play.mvc.Http;

/**
 * Created by Andrea Tortorella on 5/14/15.
 */
public class AnonymousAccessMethod implements IAccessMethod
{
    public static final AnonymousAccessMethod  INSTANCE = new AnonymousAccessMethod();
    @Override
    public boolean setCredential(Http.Context ctx)
    {
        String appcode = RequestHeaderHelper.getAppCode(ctx);
        if (Strings.isNullOrEmpty(appcode)){
            return false;
        }
        ctx.args.put("username", BBConfiguration.getBaasBoxAdminUsername());
        ctx.args.put("password",BBConfiguration.getBaasBoxAdminPassword());
        ctx.args.put("appcode",appcode);
        return true;
    }

    @Override
    public boolean isAnonymous()
    {
        return true;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }
}
