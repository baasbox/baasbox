package com.baasbox.service.logging;

import java.util.HashSet;

import play.libs.Json;
import akka.actor.UntypedActor;

import com.baasbox.service.events.EventSource;
import com.baasbox.service.events.EventsService;
import com.baasbox.service.events.EventsService.StatType;

public class LoggerActor extends UntypedActor {

	public LoggerActor() {
	}

	@Override
	public void onReceive(Object message) throws Exception {
		 if (message instanceof Registration){
	            handleRegistration((Registration)message);
	        } else if (message instanceof Unregister){
	            handleUnregister((Unregister)message);
	        } else if (message instanceof Message){
	            handleUpdate((Message)message);
	        } else {
	            unhandled(message);
	        }
	}

	private void handleUpdate(Message message) {
		EventsService.publish(StatType.SYSTEM_LOGGER, Json.toJson(message.getMessage()));
	}

	private void handleUnregister(Unregister message) {
		EventsService.removeListener(StatType.SYSTEM_LOGGER, message.source);
	}

	private void handleRegistration(Registration message) {
		EventsService.addListener(StatType.SYSTEM_LOGGER, message.source);
	}

}
