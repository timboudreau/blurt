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

import com.mastfrog.acteur.Application;
import com.mastfrog.acteur.mongo.MongoModule;
import com.mastfrog.acteur.server.ServerModule;
import com.mastfrog.acteur.util.Server;
import static com.mastfrog.blurt.collector.CollectorModule.EVENTS;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.jackson.JacksonModule;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import java.io.IOException;

/**
 *
 * @author Tim Boudreau
 */
public class CollectorApplication extends Application {

    CollectorApplication() {
        add(QueryResource.class);
        add(StreamResource.class);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Settings settings = SettingsBuilder.createWithDefaults("events").build();
        Dependencies deps = Dependencies.builder()
                .add(settings, "events")
                .add(new CollectorModule())
                .add(new JacksonModule(new JacksonMongoIDConfig()))
                .add(new MongoModule(EVENTS).bindCollection(EVENTS))
                .add(new ServerModule<CollectorApplication>(CollectorApplication.class))
                .build();

        Server server = deps.getInstance(Server.class);
        server.start(5000).await();
    }
}
