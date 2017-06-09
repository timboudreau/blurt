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
package com.mastfrog.blurt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastfrog.util.Exceptions;
import com.mastfrog.util.Streams;
import de.undercouch.bson4jackson.BsonFactory;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * BlurtCodec which uses BSON instead of JSON, resulting in smaller packets.
 * Bind BlurtCodec to this to use BSON instead of JSON for UDP packets.
 *
 * @author Tim Boudreau
 */
public final class BsonCodec implements BlurtCodec {

    private final BsonFactory factory = new BsonFactory();

    @Override
    public <T> void encode(T object, ByteBuffer into) throws IOException {
        try {
            ObjectMapper m = new ObjectMapper(factory);
            m.writeValue(Streams.asOutputStream(into), object);
        } catch (com.fasterxml.jackson.databind.JsonMappingException x) {
            if (x.getCause() != null) {
                Exceptions.chuck(x.getCause());
            } else {
                throw x;
            }
        }
    }

    @Override
    public <T> T decode(ByteBuffer buffer, Class<T> type) throws IOException {
        ObjectMapper m = new ObjectMapper(factory);
        T result = m.readValue(Streams.asInputStream(buffer), type);
        return result;
    }
}
