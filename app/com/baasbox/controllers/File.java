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

/**
 * @author Claudio Tesoriero
 *
 */
public class File extends Controller {
	  /*------------------FILE--------------------*/
	  public static Result storeFile(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result getFileMetadata(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result getFile(){
		  return status(NOT_IMPLEMENTED);
	  }
}
