package com.baasbox.service.watchers;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.RoundRobinRouter;
import com.baasbox.service.events.EventSource;
import com.baasbox.util.QueryParams;
import play.libs.Akka;

/**
 * Created by eto on 11/14/14.
 */
public class WatchService {

    private static ActorRef watch;

    public static void start(){
        Props props = Props.create(WatchManagerActor.class);
        watch = Akka.system().actorOf(props);
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

    public void publishUpdate(String collection){
        if(!watch.isTerminated()){
            watch.tell(new Update(collection),null);
        }
    }
}
