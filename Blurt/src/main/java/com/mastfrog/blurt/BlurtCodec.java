package com.mastfrog.blurt;

import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Encodes an object for transmission via Blurt
 *
 * @author Tim Boudreau
 */
@ImplementedBy(JsonCodec.class)
public interface BlurtCodec {

    <T> void encode(T object, ByteBuffer into) throws IOException;

    <T> T decode(ByteBuffer buffer, Class<T> type) throws IOException;
}
