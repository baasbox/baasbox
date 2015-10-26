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

import play.Logger;
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
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

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
								
					//Register DEVICE for user testRegisterDeviceA with os os and pushToken pushToken
					String os="os";
					String pushToken="pushToken";
					request = new FakeRequest("PUT", "/push/enable/"+os+"/"+pushToken);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					result = routeAndCall(request);
					if (Logger.isDebugEnabled()) Logger.debug("userAregisterDevice1 request: " + request.getWrappedRequest().headers());
					if (Logger.isDebugEnabled()) Logger.debug("userAregisterDevice1 result: " + contentAsString(result));
					assertRoute(result, "User A register 1st device", Status.OK, "{\"result\":\"ok\",\"data\":\"\",\"http_code\":"+Status.OK+"}", true);
				
					//Register DEVICE for user testRegisterDeviceA with os android and pushToken pushToken1
					os="os";
					pushToken="pushToken1";
					request = new FakeRequest("PUT", "/push/enable/"+os+"/"+pushToken);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					result = routeAndCall(request);
					if (Logger.isDebugEnabled()) Logger.debug("userAregisterDevice2 request: " + request.getWrappedRequest().headers());
					if (Logger.isDebugEnabled()) Logger.debug("userAregisterDevice2 result: " + contentAsString(result));
					assertRoute(result, "User A register 2nd device", Status.OK, "{\"result\":\"ok\",\"data\":\"\",\"http_code\":"+Status.OK+"}", true);
					
					ODatabaseDocumentTx db = null;
					
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
					if (Logger.isDebugEnabled()) Logger.debug("userAregisterDevice1 request: " + request.getWrappedRequest().headers());
					if (Logger.isDebugEnabled()) Logger.debug("userAregisterDevice1 result: " + contentAsString(result));
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
					
				   //com.baasbox.db.DbHelper.close(db);

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
