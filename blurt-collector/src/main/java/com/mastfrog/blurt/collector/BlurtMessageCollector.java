package com.mastfrog.blurt.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mastfrog.blurt.BlurtReceiver;
import com.mastfrog.blurt.Message;
import com.mastfrog.util.Exceptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author Tim Boudreau
 */
public class BlurtMessageCollector extends BlurtReceiver {

    private final ObjectMapper mapper;
    private final Publisher publisher;

    @Inject
    BlurtMessageCollector(ObjectMapper mapper, Publisher publisher) {
        this.mapper = mapper;
        System.out.println("Created message collector");
        this.publisher = publisher;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void receive(Message<Map<String, Object>> message) {
        System.out.println("Receive: " + message);
        try {
            String s = mapper.writeValueAsString(message);
            System.out.println("R: " + s);
            Map<String, Object> m = mapper.readValue(s, Map.class);
            DBObject obj = new BasicDBObject(m);
            publisher.publish(obj);
        } catch (JsonProcessingException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            org.openide.util.Exceptions.printStackTrace(ex);
        }
    }
}
