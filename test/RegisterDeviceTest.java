import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Test;

import com.baasbox.service.logging.BaasBoxLogger;

import play.libs.Json;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;

import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.security.SessionKeys;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;

import core.AbstractTest;
import core.TestConfig;


public class RegisterDeviceTest extends AbstractTest {
	
	
	
	@Test
	public void RegisterDevice(){
		running
		(
			getFakeApplicationWithDefaultConf(), 
			new Runnable() 
			{
				public void run() 
				{
					// Create user
					String sFakeUserA = "testRegisterDeviceA_" + UUID.randomUUID();
					String sFakeUserB = "testRegisterDeviceB_" + UUID.randomUUID();
					
										
					// Prepare test for user A
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUserA);
                    String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					String sAuthEnc = TestConfig.encodeAuth(sFakeUserA, sPwd);
					
					// Create user A
					FakeRequest request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					Result result = routeAndCall(request);
					assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUserA+"\"", true);
								
					//Register DEVICE for user testRegisterDeviceA with os 'ios' and pushToken pushToken
					String os="ios";
					String pushToken="pushToken";
					request = new FakeRequest("PUT", "/push/enable/"+os+"/"+pushToken);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					result = routeAndCall(request);
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice1 request: " + request.getWrappedRequest().headers());
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice1 result: " + contentAsString(result));
					assertRoute(result, "User A register 1st device", Status.OK, "{\"result\":\"ok\",\"data\":\"\",\"http_code\":"+Status.OK+"}", true);
				
					//Register DEVICE for user testRegisterDeviceA with os android and pushToken pushToken1
					os="android";
					pushToken="pushToken1";
					request = new FakeRequest("PUT", "/push/enable/"+os+"/"+pushToken);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					result = routeAndCall(request);
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice2 request: " + request.getWrappedRequest().headers());
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice2 result: " + contentAsString(result));
					assertRoute(result, "User A register 2nd device", Status.OK, "{\"result\":\"ok\",\"data\":\"\",\"http_code\":"+Status.OK+"}", true);
					
					ODatabaseRecordTx db = null;
					
					try{
						try {
							 db = com.baasbox.db.DbHelper.getOrOpenConnection(TestConfig.VALUE_APPCODE, TestConfig.ADMIN_USERNAME, TestConfig.AUTH_ADMIN_PASS);
						} catch (InvalidAppCodeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						List<ODocument> sqlresult = (List<ODocument>) com.baasbox.db.DbHelper.genericSQLStatementExecute("select from _BB_UserAttributes where login_info contains (pushToken = '"+pushToken+"') AND login_info contains (os = '"+os+"')",null);
						
						ODocument userA = null;
						try {
							userA = UserService.getUserProfilebyUsername(sFakeUserA);
						} catch (SqlInjectionException e) {
							Assert.fail("Error with RegisterDevice Test");
							e.printStackTrace();
						}
						
						//com.baasbox.db.DbHelper.reconnectAsAdmin();
						
						ODocument systemProps=userA.field(UserDao.ATTRIBUTES_SYSTEM);
						ArrayList<ODocument> loginInfos=systemProps.field(UserDao.USER_LOGIN_INFO);
						boolean found=false;
						
						for (ODocument loginInfo : loginInfos){
	
							if (loginInfo.field(UserDao.USER_PUSH_TOKEN).equals(pushToken) && loginInfo.field(UserDao.USER_DEVICE_OS).equals(os)){
								found=true;
							}
						}
						if (!found){
							Assert.fail("Error with test RegisterDevice");
						}
						
						// Prepare test for user B
						node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUserB);
	                    sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
						sAuthEnc = TestConfig.encodeAuth(sFakeUserB, sPwd);
						
						
						// Create user B
					    request = new FakeRequest("POST", "/user");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withJsonBody(node, "POST");
						result = routeAndCall(request);
						assertRoute(result, "routeCreateUser B check username", Status.CREATED, "name\":\""+sFakeUserB+"\"", true);
						
						
						//Register DEVICE for user testRegisterDeviceA with os os and pushToken pushToken
						os="os";
						pushToken="pushToken";
						request = new FakeRequest("PUT", "/push/enable/"+os+"/"+pushToken);
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						result = routeAndCall(request);
						if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice1 request: " + request.getWrappedRequest().headers());
						if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice1 result: " + contentAsString(result));
						assertRoute(result, "User B register device", Status.OK, "{\"result\":\"ok\",\"data\":\"\",\"http_code\":"+Status.OK+"}", true);
						
						
						try {
							userA = UserService.getUserProfilebyUsername(sFakeUserA);
						} catch (SqlInjectionException e) {
							Assert.fail("Error with RegisterDevice Test");
							e.printStackTrace();
						}
						
						systemProps=userA.field(UserDao.ATTRIBUTES_SYSTEM);
						loginInfos=systemProps.field(UserDao.USER_LOGIN_INFO);
						
						for (ODocument loginInfo : loginInfos){
	
							if (loginInfo.field(UserDao.USER_PUSH_TOKEN).equals(pushToken) && loginInfo.field(UserDao.USER_DEVICE_OS).equals(os)){
								Assert.fail("Error with test RegisterDevice");
							}
						}
					}finally{
						com.baasbox.db.DbHelper.close(db);
					}
					//admin can load the login_info structure
					
					request = new FakeRequest("GET", "/admin/user/" + sFakeUserA);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "Admin can load login_info", Status.OK, "system\":{\"login_info\":[{\"pushToken\":\"", true);

				}
			}
		);
	}

	
	@Test
	public void RegisterDeviceSameToken(){
		running
		(
			getFakeApplicationWithDefaultConf(), 
			new Runnable() 
			{
				public void run() 
				{
					// Create user
					String sFakeUserA = "testRegisterDeviceA_" + UUID.randomUUID();
										
					// Prepare test for user A
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUserA);
                    String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					String sAuthEnc = TestConfig.encodeAuth(sFakeUserA, sPwd);
					
					// Create user A
					FakeRequest request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					Result result = routeAndCall(request);
					assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUserA+"\"", true);
						
					//Register DEVICE for user testRegisterDeviceA with os 'ios' and pushToken pushToken
					String os="ios";
					String pushToken="pushToken";
					request = new FakeRequest("PUT", "/push/enable/"+os+"/"+pushToken);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					result = routeAndCall(request);
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice1 request: " + request.getWrappedRequest().headers());
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice1 result: " + contentAsString(result));
					assertRoute(result, "User A register 1st device", Status.OK, "{\"result\":\"ok\",\"data\":\"\",\"http_code\":"+Status.OK+"}", true);
				
					//Now.... Register DEVICE for user testRegisterDeviceA with os 'ios' and pushToken pushToken... AGAIN!
					request = new FakeRequest("PUT", "/push/enable/"+os+"/"+pushToken);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					result = routeAndCall(request);
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice1 request: " + request.getWrappedRequest().headers());
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("userAregisterDevice1 result: " + contentAsString(result));
					assertRoute(result, "User A register 1st device", Status.OK, "{\"result\":\"ok\",\"data\":\"\",\"http_code\":"+Status.OK+"}", true);
				
					
					ODatabaseRecordTx db = null;
					
					try{
						try {
							 db = com.baasbox.db.DbHelper.getOrOpenConnection(TestConfig.VALUE_APPCODE, TestConfig.ADMIN_USERNAME, TestConfig.AUTH_ADMIN_PASS);
						} catch (InvalidAppCodeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						List<ODocument> sqlresult = (List<ODocument>) com.baasbox.db.DbHelper.genericSQLStatementExecute("select from _BB_UserAttributes where login_info contains (pushToken = '"+pushToken+"') AND login_info contains (os = '"+os+"')",null);
						Assert.assertTrue("OS/Token not found !!", sqlresult.size() != 0);
						Assert.assertTrue("OS/Token pair is present more than once !!", sqlresult.size() == 1 );
						
						List<ODocument> sqlresultCheckUsername = (List<ODocument>) 
								com.baasbox.db.DbHelper.genericSQLStatementExecute(
										"select user.name as username from _bb_user where system = " + sqlresult.get(0).getRecord().getIdentity() ,null);
						
						Assert.assertTrue("The OS/Token pair is not belonging to any user", sqlresultCheckUsername.size() != 0);
						Assert.assertTrue("The OS/Token pair belongs to more than one user: " + OJSONWriter.listToJSON(sqlresultCheckUsername, null), sqlresultCheckUsername.size() == 1);
						Assert.assertTrue("OS/Token pair is belonging to the wrong user. Aspected: " + sFakeUserA + " found: " + sqlresultCheckUsername.get(0).field("username")
								, sqlresultCheckUsername.get(0).field("username").equals(sFakeUserA) );
						
					}finally{
						if (db != null) db.close();
					}
						
				}
			}
			);
	}
	
	
	@Override
	public String getRouteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}

}
