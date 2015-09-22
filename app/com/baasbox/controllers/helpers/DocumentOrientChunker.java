package com.baasbox.controllers.helpers;

import org.apache.commons.lang.BooleanUtils;

import play.mvc.Http.Context;

import com.baasbox.db.DbHelper;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.JSONFormats.Formats;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class DocumentOrientChunker extends AbstractOrientChunker {

	boolean withAcl = false;
	public DocumentOrientChunker(String appcode, String user, String pass,
			Context ctx) {
		//DO NOT store any ref to ctx!
		super (appcode,user,pass,ctx);
		if (ctx!=null) withAcl=BooleanUtils.toBoolean(ctx.request().getQueryString("withAcl"));
	}

	@Override
	protected String prepareDocToJson(ODocument doc) {
	        Formats format;
	        try {
	            DbHelper.filterOUserPasswords(true);
	            if (this.withAcl) {
	                format = JSONFormats.Formats.DOCUMENT_WITH_ACL;
	                return JSONFormats.prepareResponseToJson(doc, format, true);
	            } else {
	                format = JSONFormats.Formats.DOCUMENT;
	                return JSONFormats.prepareResponseToJson(doc, format, false);
	            }
	        } finally {
	            DbHelper.filterOUserPasswords(false);
	        }
	}

}
