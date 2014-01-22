package com.mastfrog.blurt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mastfrog.cluster.ApplicationInfo;
import com.mastfrog.util.Checks;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * A message received from a remote host, encapsulating the message payload
 * and information about the originating application.
 *
 * @author Tim Boudreau
 */
public final class Message<T> {
    private final SocketAddress address;
    private final T message;
    private final long when = System.currentTimeMillis();
    private final String info;

    public Message(SocketAddress address, T message, String info) {
        Checks.notNull("message", message);
        Checks.notNull("info", info);
        this.address = address;
        this.message = message;
        this.info = info;
    }
    
    @JsonProperty("addr")
    public String addressAsString() {
        return address() + "";
    }

    public SocketAddress address() {
        return address;
    }
    
    public InetSocketAddress inetAddress() {
        return address instanceof InetSocketAddress ? (InetSocketAddress) address : null;
    }
    
    @JsonProperty("msg")
    public T message() {
        return message;
    }
    
    @JsonProperty("when")
    public long when() {
        return when;
    }
    
    @Override
    public String toString() {
        return address + ":" + message;
    }
    
    @JsonProperty("info")
    public ApplicationInfo getInfo() {
        return new ApplicationInfo() {
            @Override
            @JsonProperty("app")
            public String applicationName() {
                return info.split(":")[2];
            }

            @Override
            @JsonProperty("inst")
            public String installationIdentifier() {
                return info.split(":")[0];
            }

            @Override
            @JsonProperty("proc")
            public String processIdentifier() {
                return info.split(":")[1];
            }

            @JsonIgnore
            @Override
            public String uniqueIdentifier() {
                return info;
            }
        };
    }
}
