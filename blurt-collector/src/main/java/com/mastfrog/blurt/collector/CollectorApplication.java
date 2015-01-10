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
