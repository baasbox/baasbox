package com.baasbox.controllers.helpers;

import org.apache.commons.lang.BooleanUtils;

import play.mvc.Http.Context;

import com.baasbox.db.DbHelper;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.JSONFormats.Formats;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class DocumentOrientChunker extends AbstractOrientChunker {


	public DocumentOrientChunker(String appcode, String user, String pass,
			Context ctx) {
		super (appcode,user,pass,ctx);
	}

	@Override
	protected String prepareDocToJson(ODocument doc) {
	        Formats format;
	        boolean withAcl = true;
	        if (super.getHttpContext()!=null) withAcl=BooleanUtils.toBoolean(super.getHttpContext().request().getQueryString("withAcl"));
	        try {
	            DbHelper.filterOUserPasswords(true);
	            if (withAcl) {
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
