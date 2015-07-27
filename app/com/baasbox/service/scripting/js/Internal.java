/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.service.scripting.js;
import com.baasbox.service.logging.BaasBoxLogger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Andrea Tortorella on 23/06/14.
 */
public class Internal {

    public static void log(String message){
        //todo remove
        BaasBoxLogger.debug(message);
    }

    public static void log(Object err,String msg){
    //todo remove
        if (err instanceof Throwable){
            Throwable t = (Throwable)err;
            StringWriter w = new StringWriter();
            t.printStackTrace(new PrintWriter(w));
            BaasBoxLogger.error(w.getBuffer().toString());
        }
        BaasBoxLogger.info(err.getClass().getName());
        BaasBoxLogger.info(msg);
    }
}
