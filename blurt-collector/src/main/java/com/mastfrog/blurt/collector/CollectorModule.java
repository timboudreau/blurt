/*
 * The MIT License
 *
 * Copyright 2015 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
