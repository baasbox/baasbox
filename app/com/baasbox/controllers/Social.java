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

import play.mvc.Controller;
import play.mvc.Result;

public class Social extends Controller {
	
	public static Result requestFriendship(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	public static Result acceptFriendship(){
		  return status(NOT_IMPLEMENTED);
	  }  
	
	public static Result createPost(){
		  return status(NOT_IMPLEMENTED);
	  }
	
	public static Result getCurrentUserPosts(){
		  return status(NOT_IMPLEMENTED);
	  }
	
	public static Result getUserPosts(){
		  return status(NOT_IMPLEMENTED);
	  }
	
	public static Result getPostMetadata(){
		  return status(NOT_IMPLEMENTED);
	  }
	
	public static Result getFriendList(){
		return status(NOT_IMPLEMENTED);
	  }
	
	public static Result searchUser(){
		return status(NOT_IMPLEMENTED);
	  }
	
	public static Result like(){
		return status(NOT_IMPLEMENTED);
	  }
	
	public static Result sendMessage(){
		return status(NOT_IMPLEMENTED);
	  }
	
	public static Result retrieveMessage(){
		return status(NOT_IMPLEMENTED);
	  }
	
	public static Result getMessages(){
		return status(NOT_IMPLEMENTED);
	  }
}
