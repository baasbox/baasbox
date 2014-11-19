package com.baasbox.service.watchers;

import akka.actor.UntypedActor;
import akka.dispatch.sysmsg.Watch;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.service.events.EventSource;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.orientechnologies.orient.core.record.impl.ODocument;
import scala.util.parsing.json.JSON;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 *
 * Created by eto on 11/14/14.
 */
public class WatchManagerActor extends UntypedActor {

    private static class WatchState{
        long lastUpdate;
        long count;
    }

    private Map<WatchKey,EventSource> eventSources;
    private Map<WatchKey,WatchState> statusMap;
    private Map<String,Set<WatchKey>> collToKey;

    public WatchManagerActor(){
        eventSources = new HashMap<>();
        statusMap = new HashMap<>();
        collToKey = new HashMap<>();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Registration){
            handleRegistration((Registration)message);
        } else if (message instanceof Unregister){
            handleUnregister((Unregister)message);
        } else if (message instanceof Update){
            handleUpdate((Update)message);
        } else {
            unhandled(message);
        }
    }

    private void handleUpdate(Update message) {
        long now=System.currentTimeMillis();
        Set<WatchKey> keys = collToKey.get(message.collection);
        if (keys==null||keys.isEmpty()) return;
        for (WatchKey k:keys){
            WatchState s = statusMap.get(k);
            long throttle = k.throttle();
            if (now-s.lastUpdate<throttle){
                continue;
            }
            EventSource es = eventSources.get(k);
            if (es!= null) {
                boolean update=publish(k,es,s);
                if (update) {
                    s.count++;
                    s.lastUpdate=now;
                }
            }
            if (k.updates()>0 && s.count>=k.updates()) {
                handleUnregister(new Unregister(k));
            }
        }
    }

    private void handleRegistration(Registration reg){
        WatchKey key=reg.key;
        EventSource src= reg.source;

        long now = System.currentTimeMillis();
        WatchState state = new WatchState();
        state.count = 0;
        state.lastUpdate=now;

        eventSources.put(key,src);
        statusMap.put(key, state);
        Set<WatchKey> keys = collToKey.get(key.collection);
        if (keys==null){
            keys = new HashSet<>();
            collToKey.put(key.collection,keys);
        }
        keys.add(key);

        if (key.wantsCurrent()){
            publish(key,src,state);
        }
    }

    private void handleUnregister(Unregister unregister){
        WatchKey k = unregister.key;
        statusMap.remove(k);
        EventSource src = eventSources.remove(k);
        Set<WatchKey> watchKeys = collToKey.get(k.collection);
        if (watchKeys!=null) {
            watchKeys.remove(k);
            if (watchKeys.isEmpty()) {
                collToKey.remove(k.collection);
            }
        }
        if (src!=null){
            src.close();
        }
    }


    private boolean publish(WatchKey key,EventSource src,WatchState state){
        try {
            DbHelper.open(key.appcode(),key.user(),key.password());
            QueryParams nowParams = key.params().copy();
            Date now= new Date(state.lastUpdate);
            nowParams.and("_audit.modifiedOn > ?");
            nowParams.appendParams(new Object[]{now});

            List<ODocument> documents = DocumentService.getDocuments(key.collection(), nowParams);
            if (documents.size()==0) return false;
            documents.forEach((d)->{
                String data = JSONFormats.prepareResponseToJson(d, JSONFormats.Formats.JSON);
                src.sendData(data);
            });
            return true;
        } catch (InvalidAppCodeException|SqlInjectionException|InvalidCollectionException e) {
            //todo handle errors
            return false;
        }  finally {
            DbHelper.close(DbHelper.getConnection());
        }
    }
}
