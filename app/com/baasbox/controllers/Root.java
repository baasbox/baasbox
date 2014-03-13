package com.baasbox.controllers;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.RootCredentialWrapFilter;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.metrics.BaasBoxMetric;
import com.baasbox.service.user.UserService;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Root extends Controller {

	@With({RootCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result resetAdminPassword(){
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (Logger.isDebugEnabled()) Logger.debug("resetAdminPassword bodyJson: " + bodyJson);
		//check and validate input
		if (bodyJson==null) return badRequest("The body payload cannot be empty.");		
		if (!bodyJson.has("password"))	return badRequest("The 'password' field is missing into the body");
		JsonNode passwordNode=bodyJson.findValue("password");
		
		if (passwordNode==null) return badRequest("The body payload doesn't contain password field");
		String password=passwordNode.asText();	 
		try{
			UserService.changePassword("admin", password);
		} catch (SqlInjectionException e) {
			return badRequest("The password is not valid");
		} catch (UserNotFoundException e) {
			Logger.error("User 'admin' not found!");
		    return internalServerError("User 'admin' not found!");
		}
		return ok("Admin password reset");
	}
	
   	
		@With(RootCredentialWrapFilter.class)
		public static Result timers() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service are disabled");
			ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false));
			return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getTimers()));
	    }

		@With(RootCredentialWrapFilter.class)
	    public static Result counters() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service are disabled");
	    	ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
	        return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getCounters()));
	    }

		@With(RootCredentialWrapFilter.class)
	    public static Result meters() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service are disabled");
	    	ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
	        return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getMeters()));
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result gauges() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service are disabled");
	    	ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
	        return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getGauges()));
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result histograms() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service is disabled");
	    	ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
	        return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getHistograms()));
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result uptime() throws JsonProcessingException {
	    	ObjectMapper mapper = new ObjectMapper();
	    	HashMap <String,Object> ret = new HashMap<String, Object>();
	    	ret.put("start_time", BaasBoxMetric.Track.getStartTime());
	    	ret.put("time_zone", "UTC");
	    	ret.put("uptime", BaasBoxMetric.Track.getUpTimeinMillis());
	    	ret.put("time_unit", "ms");
	        return ok(mapper.writeValueAsString(ret));
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result startMetrics() throws JsonProcessingException {
			BaasBoxMetric.start();
	        return ok("Metrics service started");
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result stopMetrics() throws JsonProcessingException {
			BaasBoxMetric.stop();
	        return ok("Metrics service stopped");
	    }
}
