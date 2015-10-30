/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.controllers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.simple.JSONObject;

import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Http.Response;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.ImagesConfiguration;
import com.baasbox.controllers.actions.filters.ConnectToDBFilterAsync;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilterAsync;
import com.baasbox.controllers.actions.filters.UserOrAnonymousCredentialsFilterAsync;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.DocumentIsNotAFileException;
import com.baasbox.exception.DocumentIsNotAnImageException;
import com.baasbox.exception.FileTooBigException;
import com.baasbox.exception.InvalidSizePatternException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.storage.FileService;
import com.baasbox.service.storage.StorageUtils;
import com.baasbox.service.storage.StorageUtils.ImageDimensions;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.ErrorToResult;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; import com.baasbox.util.BBJson;
import com.google.common.primitives.Ints;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;


public class File extends Controller {
    //TODO this should support http streaming if possible

    private static final String FILE_FIELD_NAME = "file";
    private static final String DATA_FIELD_NAME = "attachedData";
    private static final String QUERY_STRING_FIELD_DOWNLOAD = "download";
    private static final String QUERY_STRING_FIELD_RESIZE = "resize";
    private static final String QUERY_STRING_FIELD_RESIZE_ID = "sizeId";
    private static final Object ACL_FIELD_NAME = "acl";

    private static String prepareResponseToJson(ODocument doc) {
        response().setContentType("application/json");
        return JSONFormats.prepareResponseToJson(doc, JSONFormats.Formats.FILE);
    }

    private static String prepareResponseToJson(List<ODocument> listOfDoc) throws IOException {
        response().setContentType("application/json");
        return JSONFormats.prepareResponseToJson(listOfDoc, JSONFormats.Formats.FILE);
    }


    /*------------------FILE--------------------*/
    //todo cleanup
    @With({UserCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
    public static F.Promise<Result> storeFile() throws Throwable {
        MultipartFormData body = request().body().asMultipartFormData();
        if (body == null) {
            return F.Promise.pure(badRequest("missing data: is the body multipart/form-data? Check if it contains boundaries too! "));
        }
        //FilePart file = body.getFile(FILE_FIELD_NAME);
        List<FilePart> files = body.getFiles();

        final FilePart file;
        if (!files.isEmpty()) {
            file = files.get(0);
        } else {
            file = null;
        }

        String ret = "";
        if (file != null) {
            Map<String, String[]> data = body.asFormUrlEncoded();
            String[] datas = data.get(DATA_FIELD_NAME);
            String[] acl = data.get(ACL_FIELD_NAME);

			/*extract attachedData */
            String dataJson;
            if (datas != null && datas.length > 0) {
                dataJson = datas[0];
            } else {
                dataJson = "{}";
            }

				/*extract acl*/
				/*the acl json must have the following format:
				 * {
				 * 		"read" : {
				 * 					"users":[],
				 * 					"roles":[]
				 * 				 }
				 * 		"update" : .......
				 * }
				 */
            String aclJsonString;
            if (acl != null && datas.length > 0) {
                aclJsonString = acl[0];
                ObjectMapper mapper = BBJson.mapper();
                JsonNode aclJson = null;
                try {
                    aclJson = mapper.readTree(aclJsonString);
                } catch (JsonProcessingException e) {
                    return F.Promise.pure(
                            status(CustomHttpCode.ACL_JSON_FIELD_MALFORMED.getBbCode(), "The 'acl' field is malformed")
                    );
                }
					/*check if the roles and users are valid*/
                Iterator<Entry<String, JsonNode>> it = aclJson.fields();
                while (it.hasNext()) {
                    //check for permission read/update/delete/all
                    Entry<String, JsonNode> next = it.next();
                    if (!PermissionsHelper.permissionsFromString.containsKey(next.getKey())) {
                        return F.Promise.pure(status(CustomHttpCode.ACL_PERMISSION_UNKNOWN.getBbCode(), "The key '" + next.getKey() + "' is invalid. Valid ones are 'read','update','delete','all'"));
                    }
                    //check for users/roles
                    Iterator<Entry<String, JsonNode>> it2 = next.getValue().fields();
                    while (it2.hasNext()) {
                        Entry<String, JsonNode> next2 = it2.next();
                        if (!next2.getKey().equals("users") && !next2.getKey().equals("roles")) {
                            return F.Promise.pure(status(CustomHttpCode.ACL_USER_OR_ROLE_KEY_UNKNOWN.getBbCode(),
                                            "The key '" + next2.getKey() + "' is invalid. Valid ones are 'users' or 'roles'"));
                        }
                        //check for the existance of users/roles
                        JsonNode arrNode = next2.getValue();
                        if (arrNode.isArray()) {
                            for (final JsonNode objNode : arrNode) {
                                //checks the existance users and/or roles
                                if (next2.getKey().equals("users") && !UserService.exists(objNode.asText())) {
                                    return F.Promise.pure(status(CustomHttpCode.ACL_USER_DOES_NOT_EXIST.getBbCode(),
                                                             "The user " + objNode.asText() + " does not exists"));
                                }
                                if (next2.getKey().equals("roles") && !RoleService.exists(objNode.asText())) {
                                    return F.Promise.pure(status(CustomHttpCode.ACL_ROLE_DOES_NOT_EXIST.getBbCode(),
                                                                 "The role " + objNode.asText() + " does not exists"));
                                }
                            }
                        } else {
                            return F.Promise.pure(status(CustomHttpCode.JSON_VALUE_MUST_BE_ARRAY.getBbCode(),
                                    "The '" + next2.getKey() + "' value must be an array"));
                        }
                    }

                }

            } else {
                aclJsonString = "{}";
            }

            return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
                    ()->{
                        java.io.File fileContent = file.getFile();
                        String fileName = file.getFilename();
			   /*String contentType = file.getContentType();
			    if (contentType==null || contentType.isEmpty() || contentType.equalsIgnoreCase("application/octet-stream")){	//try to guess the content type
			    	InputStream is = new BufferedInputStream(new FileInputStream(fileContent));
			    	contentType = URLConnection.guessContentTypeFromStream(is);
			    	if (contentType==null || contentType.isEmpty()) contentType="application/octet-stream";
			    }*/
                        InputStream is = null;
		    	/* extract file metadata and content */
                        try {
                            is = new FileInputStream(fileContent);
                            BodyContentHandler contenthandler = new BodyContentHandler(-1);
                            //DefaultHandler contenthandler = new DefaultHandler();
                            Metadata metadata = new Metadata();
                            metadata.set(Metadata.RESOURCE_NAME_KEY, fileName);
                            Parser parser = new AutoDetectParser();
        			        try{
        			        	parser.parse(is, contenthandler, metadata,new ParseContext());
        			        }catch (Exception e){
        			        	BaasBoxLogger.warn("Could not parse the file " + fileName,e);
        			        	metadata.add("_bb_parser_error", ExceptionUtils.getMessage(e));
        			        	metadata.add("_bb_parser_exception", ExceptionUtils.getFullStackTrace(e));
        			        	metadata.add("_bb_parser_version", BBConfiguration.getInstance().getApiVersion());
        			        }
                            String contentType = metadata.get(Metadata.CONTENT_TYPE);
                            if (StringUtils.isEmpty(contentType)) contentType = "application/octet-stream";

                            HashMap<String, Object> extractedMetaData = new HashMap<String, Object>();
                            for (String key : metadata.names()) {
                                try {
                                    if (metadata.isMultiValued(key)) {
                                        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(key + ": ");
                                        for (String value : metadata.getValues(key)) {
                                            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("   " + value);
                                        }
                                        extractedMetaData.put(key.replace(":", "_").replace(" ", "_").trim(), Arrays.asList(metadata.getValues(key)));
                                    } else {
                                        if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(key + ": " + metadata.get(key));
                                        extractedMetaData.put(key.replace(":", "_").replace(" ", "_").trim(), metadata.get(key));
                                    }
                                } catch (Throwable e) {
                                    BaasBoxLogger.warn("Unable to extract metadata for file " + fileName + ", key " + key);
                                }
                            }


                            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(".................................");
                            if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(new JSONObject(extractedMetaData).toString());

                            is.close();
                            is = new FileInputStream(fileContent);
                            ODocument doc = FileService.createFile(
                                    fileName,
                                    dataJson,
                                    aclJsonString,
                                    contentType,
                                    fileContent.length(),
                                    is,
                                    extractedMetaData,
                                    contenthandler.toString());
                            return created(prepareResponseToJson(doc));
                        } catch (JsonProcessingException e) {
                            throw new Exception("Error parsing acl field. HINTS: is it a valid JSON string?", e);
                        } catch (Throwable e) {
                            BaasBoxLogger.error("Error parsing uploaded file", e);
                            throw new Exception("Error parsing uploaded file", e);
                        } finally {
                            if (is != null) is.close();
                        }

                    }));


        } else {
            return F.Promise.pure(badRequest("missing the file data in the body payload"));
        }
    }//storeFile()


    @With({UserCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
    public static F.Promise<Result> deleteFile(String id) throws Throwable {

        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
                () -> {
                    try {
                        FileService.deleteById(id);
                    } catch (FileNotFoundException e) {
                        return notFound(id + " file not found");
                    }
                    return ok();
                }));
    }

    @With({UserOrAnonymousCredentialsFilterAsync.class, ConnectToDBFilterAsync.class})
    public static F.Promise<Result> getFileAttachedData(String id) throws IOException {
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
                () -> {
                    ODocument doc = FileService.getById(id);
                    if (doc == null) {
                        return notFound(id + " file was not found");
                    } else {
                        String ret = OJSONWriter.writeValue(doc.rawField(FileService.DATA_FIELD_NAME));
                        return ok(ret);
                    }
                })).recover(ErrorToResult
                .when(SqlInjectionException.class,
                        e -> badRequest("the supplied id appears invalid (possible Sql Injection Attack detected)"))
                .when(InvalidModelException.class,
                        e -> badRequest("The id " + id + " is not a file")));
    }

    @With({UserOrAnonymousCredentialsFilterAsync.class, ConnectToDBFilterAsync.class, ExtractQueryParameters.class})
    public static F.Promise<Result> getAllFile() throws IOException {
        Context ctx = Http.Context.current.get();
        QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
        return F.Promise.promise(DbHelper.withDbFromContext(ctx,()->{
            List<ODocument> listOfFiles = FileService.getFiles(criteria);
            return ok(prepareResponseToJson(listOfFiles));
        })).recover(ErrorToResult
                .when(InvalidCriteriaException.class,
                        e -> badRequest(e.getMessage() != null ? e.getMessage() : ""))
                .when(SqlInjectionException.class,
                        e -> badRequest("the supplied criteria appear invalid (Sql Injection Attack detected)")));
    }

    @With({UserOrAnonymousCredentialsFilterAsync.class, ConnectToDBFilterAsync.class, ExtractQueryParameters.class})
    public static F.Promise<Result> getFileContent(String id) {
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
                ()->{
                    response().setHeader(Response.CONTENT_TYPE,"text/plain");
                    return ok(FileService.getExtractedContent(id));
                })).recover(ErrorToResult
                .when(SqlInjectionException.class,
                        e -> badRequest("The querystring is malformed or not well encoded"))
                .when(InvalidModelException.class,
                        e -> badRequest("The id " + id + " is not a file"))
                .when(FileNotFoundException.class,
                        e -> notFound("The file " + id + " was not found")));
    }

    @With({UserOrAnonymousCredentialsFilterAsync.class, ConnectToDBFilterAsync.class, ExtractQueryParameters.class})
    public static F.Promise<Result> getFile(String id) {
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
                ()-> {
                    ODocument doc = FileService.getById(id);
                    if (doc == null) {
                        return notFound(id + " file was not found");
                    } else {
                        return ok(prepareResponseToJson(doc));
                    }
                })).recover(ErrorToResult
                .when(SqlInjectionException.class,
                        e -> badRequest("the supplied id appears invalid (possible Sql Injection Attack detected)"))
                .when(InvalidModelException.class,
                        e -> badRequest("The id " + id + " is not a file")));
    }

    @With({UserOrAnonymousCredentialsFilterAsync.class, ConnectToDBFilterAsync.class})
    public static F.Promise<Result> streamFile(String id) throws IOException, InvalidModelException {
        Context ctx = Http.Context.current.get();
        String resize = ctx.request().getQueryString(QUERY_STRING_FIELD_RESIZE);
        boolean resizeIsEmpty = StringUtils.isEmpty(resize);
        Integer sizeId = Ints.tryParse(ctx.request().getQueryString(QUERY_STRING_FIELD_RESIZE_ID) + "");
        Boolean download = BooleanUtils.toBoolean(ctx.request().getQueryString(QUERY_STRING_FIELD_DOWNLOAD));

        return F.Promise.promise(DbHelper.withDbFromContext(ctx,
                ()->{
                    ODocument doc = FileService.getById(id);
                    if (doc == null) {
                        return notFound(id + " file was not found");
                    }
                    String filename = doc.<String>field("fileName");
                    byte[] output;
                    if (sizeId != null) {
                        output = FileService.getResizedPicture(id,sizeId);
                        String[] fileName = ((String) doc.field("fileName")).split("\\.");
                        filename = fileName[0] + "_" + sizeId + "." + (fileName.length > 1 ? fileName[1] : "");
                    } else if (!resizeIsEmpty) {
                        if (!ImagesConfiguration.IMAGE_ALLOWED_AUTOMATIC_RESIZE_FORMATS.getValueAsString().contains(resize)
                             && !UserService.userCanByPassRestrictedAccess(DbHelper.currentUsername())) {
                            throw new InvalidSizePatternException("The requested resize format is not allowed");
                        }
                        ImageDimensions imgDim = StorageUtils.convertPatternToDimensions(resize);
                        output = FileService.getResizedPicture(id, imgDim);
                        String[] fileName = ((String) doc.field("fileName")).split("\\.");
                        filename = fileName[0] + "_" + resize + "." + (fileName.length > 1 ? fileName[1] : "");
                    } else {
                        ORecordBytes record = doc.field(FileService.BINARY_FIELD_NAME);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        record.toOutputStream(out);
                        output = out.toByteArray();
                    }
                    response().setContentType((String) doc.field(FileService.CONTENT_TYPE_FIELD_NAME));
                    response().setHeader("Content-Length", String.valueOf(output.length));
                    if (download) {
                        response().setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(filename, "UTF-8") + "\"");
                    }
                    return ok(output);

                })).recover(ErrorToResult
            .when(SqlInjectionException.class,
                    e -> badRequest("the supplied id appears invalid (Sql Injection Attack detected)"))
            .when(IOException.class,
                    e ->{
                        BaasBoxLogger.error("error retrieving file content " + id, e);
                        return internalServerError(ExceptionUtils.getMessage(e));
                    })
            .when(DocumentIsNotAnImageException.class,
                    e -> badRequest("The id " + id + "is not an image and cannot be resize"))
            .when(DocumentIsNotAFileException.class,
                    e -> badRequest("The id " + id + "is not a file"))
            .when(InvalidSizePatternException.class,
                    e -> badRequest("The resize parameters are not valid"))
            .when(InvalidModelException.class,
                    e -> internalServerError(e.getMessage()))
            .when(DocumentNotFoundException.class,
                    e -> notFound("The requested file does not exists: " + id))
            .when(FileTooBigException.class,
                    e -> status(503, "The requested image is too big to be processed now. Try later. File: " + id))
            );
    }//streamFile


    @With({UserCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class, ExtractQueryParameters.class})
    public static F.Promise<Result> grantOrRevokeToRole(String id, String rolename, String action, boolean grant) {
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
                ()->{
                    Permissions permission = PermissionsHelper.permissionsFromString.get(action.toLowerCase());
                    if (grant){
                        FileService.grantPermissionToRole(id, permission, rolename);
                    } else {
                        FileService.revokePermissionToRole(id, permission, rolename);
                    }
                    return ok();

                })).recover(ErrorToResult
                    .when(IllegalArgumentException.class,
                            e -> badRequest(ExceptionUtils.getMessage(e)))
                    .when(RoleNotFoundException.class,
                            e -> notFound("role " + rolename + " not found"))
                    .when(OSecurityAccessException.class,
                            e -> forbidden())
                    .when(OSecurityException.class,
                            e -> forbidden())
                    .when(Throwable.class,
                            e -> internalServerError(ExceptionUtils.getMessage(e))));
    }//grantOrRevokeToRole

    @With({UserCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class, ExtractQueryParameters.class})
    public static F.Promise<Result> grantOrRevokeToUser(String id, String username, String action, boolean grant) {
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
                () ->{
                    Permissions permission = PermissionsHelper.permissionsFromString.get(action.toLowerCase());
                    if (grant) {
                        FileService.grantPermissionToUser(id, permission, username);
                    }
                    else {
                        FileService.revokePermissionToUser(id, permission, username);
                    }
                    return ok();
                })).recover(ErrorToResult
                .when(IllegalArgumentException.class,
                        e -> badRequest(ExceptionUtils.getMessage(e)))
                .when(RoleNotFoundException.class,
                        e -> notFound("user " + username + " not found"))
                .when(OSecurityAccessException.class,
                        e -> forbidden())
                .when(OSecurityException.class,
                        e -> forbidden())
                .when(Throwable.class,
                        e -> internalServerError(e.getMessage()==null?"":ExceptionUtils.getMessage(e))));
    }//grantOrRevokeToUser


    public static Result updateAttachedData() {
        return status(NOT_IMPLEMENTED);
    }


}