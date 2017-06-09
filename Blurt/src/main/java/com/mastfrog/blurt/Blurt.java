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

import com.google.inject.ImplementedBy;

/**
 * A generic fire-and-forget messaging system which allows an application to
 * fire small packets of information off into the ether. Useful for cluster
 * activity, monitoring, and who knows what else.
 *
 * @author Tim Boudreau
 */
@ImplementedBy(BlurtUDP.class)
public interface Blurt {

    //Settings keys
    public static final String BLURT_LOOPBACK_ONLY = "blurt.loopback.only";
    public static final String BLURT_NAME = "blurt.name";
    public static final String BLURT_UDP_PORT = "blurt.udp.port";
    public static final String BLURT_RECEIVE = "blurt.receive";
    public static final String BLURT_SEND = "blurt.send";
    public static final String BLURT_ENABLED = "blurt.enabled";
    public static final String BLURT_UDP_IPV6 = "blurt.upd.ipv6";
    public static final String BLURT_UDP_HOST = "blurt.udp.host";
    public static final String BLURT_UDP_THREAD_COUNT = "blurt.udp.thread.count";
    public static final String BLURT_HEARTBEAT = "blurt.heartbeat";
    public static final String BLURT_UDP_NETWORK_INTERFACE = "blurt.udp.network.interface";
    public static final String BLURT_UDP_MULTICAST_LOOPBACK = "blurt.udp.multicast.loopback";
    public static final String BLURT_UDP_BROADCAST = "blurt.udp.broadcast";
    public static final String BLURT_UDP_MULTICAST_TTL = "blurt.udp.multicast.ttl";
    public static final String BLURT_UDP_TRAFFIC_CLASS = "blurt.udp.traffic.class";
    public static final String BLURT_UDP_BUFFER_SIZE = "blurt.udp.buffer.size";
    public static final String BLURT_AUTOSTART = "blurt.autostart";
    public static final String BLURT_HEARTBEAT_INTERVAL_MILLIS = "blurt.heartbeat.interval";
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 30000;

    /**
     * Blurt a message out into the ether
     *
     * @param message The message (should be encodable as JSON or similar)
     * @return True if the message was sent (if blurt is disabled or closed,
     * will be false)
     */
    boolean blurt(Object message);

    /**
     * Blurt a message out into the ether
     *
     * @param key The name of the thing being blurted (will be key:value in
     * json)
     * @param value The message (should be encodable as JSON or similar)
     * @return True if the message was sent (if blurt is disabled or closed,
     * will be false)
     */
    boolean blurt(String key, Object value);
}
