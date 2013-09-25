package com.baasbox.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import play.Logger;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.configuration.Internal;
import com.baasbox.configuration.SocialLoginConfiguration;
import com.baasbox.controllers.actions.filters.AdminCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.UserDao;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.service.sociallogin.SocialLoginService;
import com.baasbox.service.sociallogin.UserInfo;
import com.baasbox.service.user.UserService;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Social extends Controller{


	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result login(String socialNetwork){
		String keyFormat = socialNetwork.toUpperCase()+"_ENABLED";
		Boolean enabled = SocialLoginConfiguration.valueOf(keyFormat).getValueAsBoolean();
		if(enabled==null || enabled == false){
			return badRequest("Social login for "+socialNetwork+" is not enabled");
		}else{
			SocialLoginService sc = SocialLoginService.by(socialNetwork,(String)ctx().args.get("appcode"));
			return ok("{\"url\":\""+sc.getAuthorizationURL(session())+"\"}");

		}

	}

	/**
	 * This method is a common callback for all oauth
	 * providers.It isn't annotated with a Filter because
	 * social networks callback requests couldn't pass the 
	 * auth headers needed by baasbox.
	 * @param socialNetwork
	 * @return
	 */
	public static Result callback(String socialNetwork){
		SocialLoginService sc = SocialLoginService.by(socialNetwork,(String)ctx().args.get("appcode"));
		Token t = sc.requestAccessToken(request(),session());
		return ok("{\"oauth_token\":\""+t.getToken()+"\",\"oauth_secret\":\""+t.getSecret()+"\"}");
	}

	/**
	 * Login the user through socialnetwork specified
	 * 
	 * An oauth_token and oauth_secret provided by oauth steps
	 * are mandatory 
	 * @param socialNetwork
	 * @return
	 */
	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result loginWith(String socialNetwork){

		String authToken = request().getQueryString("oauth_token");
		String authSecret = request().getQueryString("oauth_secret");

		if(StringUtils.isEmpty(authToken) || StringUtils.isEmpty(authSecret)){
			return badRequest("Both oauth_token and oauth_secret should be specified in the query string");
		}

		String appcode = (String)ctx().args.get("appcode");
		SocialLoginService sc = SocialLoginService.by(socialNetwork,appcode);
		Token t = new Token(authToken,authSecret);
		UserInfo result = sc.getUserInfo(t);
		result.setFrom(socialNetwork);
		result.setToken(t.getToken());
		result.setGeneratedUsername(true);
		//Setting token as secret for one-token only social networks
		result.setSecret(t.getSecret()!=null && StringUtils.isNotEmpty(t.getSecret())?t.getSecret():t.getToken());
		UserDao userDao = UserDao.getInstance();
		ODocument existingUser =  null;
		try{
			existingUser = userDao.getBySocialUserId(result);
			
		}catch(SqlInjectionException sie){
			return internalServerError(sie.getMessage());
		}

		if(existingUser!=null){
			
			
			String username = sc.getPrefix()+result.getId();
			String password = generateUserPassword(username, (Date)existingUser.field(UserDao.USER_SIGNUP_DATE));
			UserInfo firstSocialLogin =findFirstGenerated(existingUser);
			if(firstSocialLogin!=null){
				//TODO: this should be moved to an enum
				String prefix = sc.getPrefixByName(firstSocialLogin.getFrom());
				String id = firstSocialLogin.getId();
				username = prefix+id;
				password = generateUserPassword(username, (Date)existingUser.field(UserDao.USER_SIGNUP_DATE));
			}
			
			ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode,username, password);
			response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
			ObjectNode on = Json.newObject();
			if(existingUser!=null){
				on.put("user", Json.parse( User.prepareResponseToJsonUserInfo(existingUser)).get("user"));
			}
			on.put(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
			return ok(on);
		}else{
			Logger.debug("User does not exists with tokens...trying to create");
			String username = sc.getPrefix()+result.getId();
			Date signupDate = new Date();
			try{
				String password = generateUserPassword(username, signupDate);
				JsonNode privateData = null;
				if(result.getAdditionalData()!=null && !result.getAdditionalData().isEmpty()){
					privateData = Json.toJson(result.getAdditionalData());
				}
				ODocument profile = UserService.signUp(username, password, signupDate, null, privateData, null, null);
				UserService.addSocialLoginTokens(profile,result,true);
				ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode, username, password);
				response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
				ObjectNode on = Json.newObject();
				if(profile!=null){
					on.put("user", Json.parse( User.prepareResponseToJsonUserInfo(profile)).get("user"));
				}
				on.put(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));

				return ok(on);
			}catch(Exception uaee){
				return internalServerError(uaee.getMessage());
			}
		}
	}
	
	private static UserInfo findFirstGenerated(ODocument existingUser) {
		Map<String,ODocument> socialLogins = existingUser.field(UserDao.ATTRIBUTES_SYSTEM+"."+UserDao.SOCIAL_LOGIN_INFO);
		UserInfo result = null;
		if(socialLogins!=null){
			for (ODocument socialLogin : socialLogins.values()) {
				UserInfo temp = UserInfo.fromJson(socialLogin.toJSON());
				if(temp.isGeneratedUsername()){
					result = temp;
					break;
				}
			}
		}
		return result;
	}

	@With ({UserCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result linkWith(String socialNetwork){
		String authToken = request().getQueryString("oauth_token");
		String authSecret = request().getQueryString("oauth_secret");

		if(StringUtils.isEmpty(authToken) || StringUtils.isEmpty(authSecret)){
			return badRequest("Both oauth_token and oauth_secret should be specified in the query string");
		}

		String appcode = (String)ctx().args.get("appcode");
		SocialLoginService sc = SocialLoginService.by(socialNetwork,appcode);
		Token t = new Token(authToken,authSecret);
		UserInfo result = sc.getUserInfo(t);
		result.setFrom(socialNetwork);
		result.setToken(t.getToken());
		
		//Setting token as secret for one-token only social networks
		result.setSecret(t.getSecret()!=null && StringUtils.isNotEmpty(t.getSecret())?t.getSecret():t.getToken());
		ODocument user;
		try {
			user = UserService.getCurrentUser();
			ODocument other = UserDao.getInstance().getBySocialUserId(result);
			boolean sameUser = other!=null && other.getIdentity().equals(user.getIdentity());
			if(other==null || !sameUser){
				UserService.addSocialLoginTokens(user, result, false);
			}else{
				internalServerError("A user with this token already exists and it's not the current user.");
			}
			return ok();
		} catch (SqlInjectionException e) {
			return internalServerError(e.getMessage());
		}
		
		
		
	}
	

	private static String generateUserPassword(String username,Date signupDate){
		String bbid=Internal.INSTALLATION_ID.getValueAsString();
		String password = Crypto.sign(username+new SimpleDateFormat("ddMMyyyyHHmmss").format(signupDate)+bbid);
		return password;
	}


}
