package com.baasbox.service.watchers;

import akka.actor.*;
import akka.pattern.Patterns;
import com.baasbox.service.events.EventSource;
import com.baasbox.util.QueryParams;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by eto on 11/14/14.
 */
public class WatchService {

    private static volatile ActorRef watch;

    public static void start(){
        Props props = Props.create(WatchManagerActor.class);
        watch = Akka.system().actorOf(props);
        Logger.info("Started dispatcher: "+watch);
    }

    public static WatchKey makeKey(String appcode,String user,String pass,String collection,QueryParams params,String throttle,String updates,String current){
        WatchKey key = new WatchKey(appcode,user,pass,collection,params,current,throttle,updates);
        return key;
    }

    public static void registerWatchKey(WatchKey k,EventSource es){
        if (!watch.isTerminated()){
            watch.tell(new Registration(k,es),null);
        }
    }

    public static void unregisterWatchKey(WatchKey key){
       if(!watch.isTerminated()) {
           watch.tell(new Unregister(key),null);
       }
    }

    public static void publishUpdate(String collection){
        Scheduler sched = Akka.system().scheduler();
        Update up = new Update(collection);
        FiniteDuration time = Duration.create(100, TimeUnit.MILLISECONDS);
        ExecutionContext ctx = Akka.system().dispatcher();

        DEBOUNCED.computeIfAbsent(collection, (c) -> sched.scheduleOnce(time, () -> {
            watch.tell(up,null);
            DEBOUNCED.remove(collection);
        }, ctx));
    }

    private static final ConcurrentHashMap<String,Cancellable> DEBOUNCED = new ConcurrentHashMap<>();


    public static void stop() {
        if (watch != null){
            Future<Boolean> stopped = Patterns.gracefulStop(watch, Duration.create(1, TimeUnit.SECONDS));
            try{
                Await.result(stopped,Duration.create(1100,TimeUnit.MILLISECONDS));
            }catch (Exception e){
                Logger.warn("Watchers not stopped gracefully");
            }
        }
    }

}
