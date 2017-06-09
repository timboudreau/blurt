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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mastfrog.jackson.JacksonConfigurer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.bson.types.ObjectId;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = JacksonConfigurer.class)
public class JacksonMongoIDConfig implements JacksonConfigurer {

    @Override
    public ObjectMapper configure(ObjectMapper om) {
        om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        SimpleModule sm = new SimpleModule("mongo", new Version(1, 0, 0, null, "com.timboudreau", "trackerapi"));
        sm.addSerializer(new C());
        sm.addDeserializer(ObjectId.class, new D());
        om.registerModule(sm);
        return om;
    }

    static class C extends JsonSerializer<ObjectId> {

        @Override
        public Class<ObjectId> handledType() {
            return ObjectId.class;
        }

        @Override
        public void serialize(ObjectId t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String id = t.toStringMongod();
            jg.writeString(id);
        }
    }

    static class D extends JsonDeserializer<ObjectId> {

        @Override
        public Class<?> handledType() {
            return ObjectId.class;
        }

        @Override
        public ObjectId deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String id = jp.getValueAsString();
            return new ObjectId(id);
        }
    }
}
