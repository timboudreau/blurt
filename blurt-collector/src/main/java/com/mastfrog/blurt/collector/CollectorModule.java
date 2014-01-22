package com.mastfrog.blurt.collector;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.mastfrog.acteur.mongo.MongoInitializer;
import com.mastfrog.blurt.BlurtCodec;
import com.mastfrog.blurt.BlurtControl;
import com.mastfrog.blurt.BlurtReceiver;
import com.mastfrog.blurt.BsonCodec;
import com.mongodb.DBCollection;
import java.io.IOException;
import java.net.SocketException;

/**
 *
 * @author Tim Boudreau
 */
public class CollectorModule extends AbstractModule {

    public static final String EVENTS = "events";

    @Override
    protected void configure() {
        bind(MI.class).asEagerSingleton();
        bind(BlurtReceiver.class).to(BlurtMessageCollector.class).asEagerSingleton();
        bind(Starter.class).asEagerSingleton();
        bind(BlurtCodec.class).to(BsonCodec.class);
    }

    private static class MI extends MongoInitializer {
        @Inject
        public MI(Registry registry) {
            super(registry);
        }

        @Override
        protected void onCreateCollection(DBCollection collection) {
            super.onCreateCollection(collection);
        }
    }
    
    private static class Starter {
        @Inject
        Starter(BlurtControl blurt) throws SocketException, IOException {
            blurt.start();
        }
    }
}
