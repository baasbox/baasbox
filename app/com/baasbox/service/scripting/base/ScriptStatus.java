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

package com.baasbox.service.scripting.base;

/**
 * Created by Andrea Tortorella on 04/07/14.
 */
public class ScriptStatus {
    public final boolean ok;
    public final String message;

    private static final ScriptStatus OK = new ScriptStatus(true,"");
    private static final ScriptStatus FAIL = new ScriptStatus(false,"");

    private ScriptStatus(boolean ok,String message){
        this.ok=ok;
        this.message=message;
    }

    public static ScriptStatus ok(){
        return OK;
    }

    public static ScriptStatus ok(String message){
        return new ScriptStatus(true,message);
    }

    public static ScriptStatus fail(){
        return FAIL;
    }

    public static ScriptStatus fail(String message){
        return new ScriptStatus(false,message);
    }
}
