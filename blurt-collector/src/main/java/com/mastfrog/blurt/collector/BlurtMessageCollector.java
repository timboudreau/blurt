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
