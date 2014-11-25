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

package com.baasbox.commands.exceptions;

import com.baasbox.commands.exceptions.CommandException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by Andrea Tortorella on 30/06/14.
 */
public class ResourceNotFoundException extends CommandNotFoundException {

    public ResourceNotFoundException(JsonNode command, String message) {
        super(command, message);
    }

    public ResourceNotFoundException(JsonNode command, String message, Throwable cause) {
        super(command, message, cause);
    }
}
