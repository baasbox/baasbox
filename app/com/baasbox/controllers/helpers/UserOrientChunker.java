package com.baasbox.controllers.helpers;

import play.mvc.Http.Context;

import com.baasbox.util.JSONFormats;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class UserOrientChunker extends AbstractOrientChunker {

  public UserOrientChunker(String appcode, String user, String pass, Context ctx) {
    super(appcode, user, pass, ctx);
  }

  @Override
  protected String prepareDocToJson(ODocument doc) {
    return JSONFormats.prepareResponseToJson(doc, JSONFormats.Formats.USER);
  }

}
