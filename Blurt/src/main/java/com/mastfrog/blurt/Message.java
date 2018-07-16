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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mastfrog.cluster.ApplicationInfo;
import com.mastfrog.util.preconditions.Checks;
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
