/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
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

package com.baasbox.service.permissions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import play.Logger;
import play.api.Routes;
import play.mvc.Http;
import scala.Option;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for parsing annotations
 * Created by Andrea Tortorella on 08/04/14.
 */
public final class RouteTagger {
    private RouteTagger(){throw new AssertionError("uninstantiable");}

    // change to true to check route parse logging
    private static final boolean CHECK_PARSING = false;

    private static final Pattern COMMENT_ANNOTATION_PATTERN =
            Pattern.compile("@([a-zA-Z_$][a-zA-Z\\d_$]*)(\\((([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*)?\\))?");


    private static final LoadingCache<String,Map<String,Set<String>>> PARSER_CACHE =
            CacheBuilder.newBuilder()
                        .build(new CacheLoader<String, Map<String, Set<String>>>() {
                            @Override
                            public Map<String, Set<String>> load(String comment) throws Exception {
                                if (Logger.isDebugEnabled()) Logger.debug("New route annotations to parse ");
                                return parseComments(comment);
                            }
                        });

    /**
     * Attaches route annotations to the context for the current request
     * @param context
     * @return the attached annotations map
     */
    public static Map<String,Set<String>> attachAnnotations(Http.Context context){
        final String routeComment = getComment(context);
        Map<String, Set<String>> annotations = parse(routeComment);
        context.args.putAll(annotations);
        Http.Context.current.set(context);
        return annotations;
    }

    @VisibleForTesting
    public static Map<String,Set<String>> parse(final String comment){
        try {
            if (Logger.isDebugEnabled()) Logger.debug("Parsing tags for current route with comment: "+comment);
            if (comment==null||comment.length()==0){
                return Collections.emptyMap();
            }
            if (Logger.isDebugEnabled()) Logger.debug("Matched routes");
            return PARSER_CACHE.get(comment);
        } catch (ExecutionException e){
            if (Logger.isErrorEnabled())Logger.error("Error reading tags for current route with comment: "+comment);
            return Collections.emptyMap();
        }
    }

    private static Map<String,Set<String>> parseComments(String comment){
        HashMap<String,Set<String>> tags = new HashMap<String,Set<String>>();
        Matcher m = COMMENT_ANNOTATION_PATTERN.matcher(comment);
        while (m.find()){
            int groups = m.groupCount();
            if (CHECK_PARSING) {
                Logger.info("Comment: " + comment);
                for (int i = 0; i < groups; i++) {
                    Logger.info("group(" + i + "): " + m.group(i));
                }
            }
            String name = m.group(1);
            String value = groups>2?m.group(3):name;
            if (value==null){
                value=name;
            }
            Set<String> vals = tags.get(name);
            if (vals == null){
                vals = new HashSet<String>();
                tags.put(name,vals);
            }
            vals.add(value);
        }
        return tags;
    }

    private static String getComment(Http.Context context){
        Option<String> opt = context._requestHeader().tags().get(Routes.ROUTE_COMMENTS());
        return opt.isDefined()?opt.get():"";
    }
}
