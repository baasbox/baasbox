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

import play.Play;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Scanner;

/**
 * Created by Andrea Tortorella on 10/06/14.
 */
class ResLoader {
    private static final String FOLDER = "scripts/";
    private static final String PRELUDE = "_baasbox_prelude.js";

    static Reader jsPrelude(){
        InputStream in = Play.application().resourceAsStream(FOLDER+PRELUDE);
        return new InputStreamReader(in);
    }

    static String loadJsScript(String name){
        String fileName = FOLDER+name.replaceAll("\\.", "/")+".js";
        InputStream in = Play.application().resourceAsStream(fileName);
        if (in == null) return null;
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext()?s.next():"";
    }

}
