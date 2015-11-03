package com.baasbox.util;


import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Predef;

import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by eto on 12/19/14.
 */
public class ErrorToResult {


    public static <T extends Throwable> Handler when(Class<T> t,F.Function<T,Result> action){
        return new Handler().when(t,action);
    }


    //todo linear search of the matching exception can be logn
    public static class Handler implements F.Function<Throwable,Result>{
        private ArrayList<Element> handlers = new ArrayList<>();

        private static class Element{
            private final Class<? extends Throwable> clazz;
            private final F.Function<Throwable,Result> handler;

            public Element(Class<? extends Throwable> clazz, F.Function<Throwable, Result> handler) {
                this.clazz = clazz;
                this.handler = handler;
            }
        }
        private Handler(){

        }

        @Override
        public Result apply(Throwable throwable) throws Throwable {
            for (Element e: handlers){
                if (e.clazz.isAssignableFrom(throwable.getClass())){
                    return e.handler.apply(throwable);
                }
            }
            return Controller.internalServerError(throwable.getMessage());
        }

        public <T extends Throwable> Handler when(Class<T> t, F.Function<T, Result> action) {
            handlers.add(new Element(t, (F.Function<Throwable, Result>) action));
            return this;
        }
    }


}
