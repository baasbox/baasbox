package com.baasbox.service.logging;

import com.baasbox.service.events.EventSource;

public class Unregister {
	public final EventSource source;

	Unregister(EventSource source){
        this.source=source;
    }
}
