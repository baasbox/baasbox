package com.baasbox.service.logging;

import com.baasbox.service.events.EventSource;

public class Registration {
	public final EventSource source;

    Registration(EventSource source){
        this.source=source;
    }
}
