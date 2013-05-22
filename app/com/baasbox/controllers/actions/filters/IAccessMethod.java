package com.baasbox.controllers.actions.filters;

import play.mvc.Http.Context;

public interface IAccessMethod {

	public boolean setCredential(Context ctx);

}