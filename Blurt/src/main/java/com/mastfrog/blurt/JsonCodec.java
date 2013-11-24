package com.mastfrog.blurt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mastfrog.util.Exceptions;
import com.mastfrog.util.Streams;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
final class JsonCodec implements BlurtCodec {

    private final ObjectMapper mapper;

    @Inject
    JsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> void encode(T object, ByteBuffer into) throws IOException {
        try {
            mapper.writeValue(Streams.asOutputStream(into), object);
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
        T result = mapper.readValue(Streams.asInputStream(buffer), type);
        return result;
    }
}
