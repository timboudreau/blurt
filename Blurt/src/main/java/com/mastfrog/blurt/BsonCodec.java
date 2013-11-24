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
