package com.baasbox.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
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
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.service.sociallogin.SocialLoginService;
import com.baasbox.service.sociallogin.SocialLoginService.Tokens;
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
		//Setting token as secret for facebook
		result.setSecret(t.getSecret()!=null && StringUtils.isNotEmpty(t.getSecret())?t.getSecret():t.getToken());
		UserDao userDao = UserDao.getInstance();
		ODocument existingUser = null;
		try{
			existingUser = userDao.getBySocialUserId(result);
			if(existingUser!=null){
				Logger.debug("Found a user with tokens");
				String password = generateUserPassword(result.getUsername(), (Date)existingUser.field(UserDao.USER_SIGNUP_DATE));
				ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode, result.getUsername(), password);
				response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
				ObjectNode on = Json.newObject();
				if(existingUser!=null){
					on.put("user", Json.parse( User.prepareResponseToJsonUserInfo(existingUser)).get("user"));
				}
				on.put(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
				return ok(on);

			}
		}catch(SqlInjectionException sie){
			return internalServerError(sie.getMessage());
		}
		if(existingUser==null){

			Logger.debug("User does not exists with tokens...trying to create");
			String username = result.getUsername();
			Date signupDate = new Date();
			String password = null;
			
			try{
				ODocument profile = UserDao.getInstance().getByUserName(username);
				if(profile!=null){
					Logger.debug("find a user with "+result.getUsername());
					password = generateUserPassword(result.getUsername(),(Date)profile.field(UserDao.USER_SIGNUP_DATE));
				}else{
					password = generateUserPassword(username, signupDate);
					profile = UserService.signUp(username, password, signupDate, null, null, null, null);
				}
				UserService.addSocialLoginTokens(profile,socialNetwork,result.getId(),true);
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
		return TODO;




	}

	private static String generateUserPassword(String username,Date signupDate){
		String bbid=Internal.INSTALLATION_ID.getValueAsString();
		String password = Crypto.sign(username+new SimpleDateFormat("ddMMyyyyHHmmss").format(signupDate)+bbid);
		return password;
	}


}
