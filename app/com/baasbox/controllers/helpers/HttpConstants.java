package com.baasbox.controllers.helpers;

public interface HttpConstants {
	public interface HttpProtocol {
		 // Versions
		  String HTTP_1_0 = "HTTP/1.0";
		  String HTTP_1_1 = "HTTP/1.1";

		  // Other HTTP protocol values
		  String CHUNKED = "chunked";
	}
	
	public interface Headers {
		String TRANSFER_ENCODING = "Transfer-Encoding";
	}
	
	
}
