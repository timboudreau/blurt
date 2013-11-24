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
