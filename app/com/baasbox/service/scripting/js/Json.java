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

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.io.IOException;

/**
 * Created by Andrea Tortorella on 23/06/14.
 */
public class Json {
    private static final ObjectMapperExt MAPPER = new ObjectMapperExt();

    public static class ObjectMapperExt  extends ObjectMapper{

        public JsonNode readTreeOrMissing(String in){
            try {
                return readTree(in);
            } catch (IOException e) {
                return MissingNode.getInstance();
            }
        }
    }

    public static String prettyPrinted(JsonNode value){
        try {
            return mapper().writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
    public static ObjectMapperExt mapper(){
        return MAPPER;
    }
}
