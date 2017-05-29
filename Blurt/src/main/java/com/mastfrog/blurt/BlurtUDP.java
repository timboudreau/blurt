package com.mastfrog.blurt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import static com.mastfrog.blurt.Blurt.BLURT_AUTOSTART;
import static com.mastfrog.blurt.Blurt.BLURT_ENABLED;
import static com.mastfrog.blurt.Blurt.BLURT_HEARTBEAT;
import static com.mastfrog.blurt.Blurt.BLURT_HEARTBEAT_INTERVAL_MILLIS;
import static com.mastfrog.blurt.Blurt.BLURT_LOOPBACK_ONLY;
import static com.mastfrog.blurt.Blurt.BLURT_RECEIVE;
import static com.mastfrog.blurt.Blurt.BLURT_SEND;
import static com.mastfrog.blurt.Blurt.BLURT_UDP_HOST;
import static com.mastfrog.blurt.Blurt.BLURT_UDP_IPV6;
import static com.mastfrog.blurt.Blurt.BLURT_UDP_NETWORK_INTERFACE;
import static com.mastfrog.blurt.Blurt.BLURT_UDP_PORT;
import static com.mastfrog.blurt.Blurt.BLURT_UDP_THREAD_COUNT;
import static com.mastfrog.blurt.Blurt.DEFAULT_HEARTBEAT_INTERVAL;
import static com.mastfrog.blurt.BlurtUDP.DEFAULT_IPV6_UDP_HOST;
import static com.mastfrog.blurt.BlurtUDP.DEFAULT_UDP_HOST;
import static com.mastfrog.blurt.BlurtUDP.DEFAULT_UDP_PORT;
import static com.mastfrog.blurt.BlurtUDP.logger;
import com.mastfrog.cluster.ApplicationInfo;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.giulius.annotations.Defaults;
import com.mastfrog.settings.Settings;
import com.mastfrog.util.Checks;
import com.mastfrog.util.collections.CollectionUtils;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A way to blurt UDP packets into the ether. The idea is to simply pass in
 * strings or objects, and the system-wide blurt instance will fire-and-forget
 * them into the either. Then another application on the same network can pick
 * the packets up and turn them into whatever it wants (a log, an activity
 * monitor, statistics, you name it).
 * <p/>
 * Each blurt packet contains a timestamp and two identifying GUIDs: An
 * "installation guid" (stored in ~/.appname), which is consistent across runs,
 * and a "process guid" which is a unique ID of a specific process.
 * <p/>
 * IPv4 and IPv6 are supported. If you're using IPv4, packets may be truncated
 * if they are longer than 512 bytes; if you want to blurt large stuff, use
 * IPv6.
 *
 * @author Tim Boudreau
 */
//@Defaults(value = {BLURT_UDP_HOST + "=224.0.0.1",
//    BLURT_UDP_PORT + "=41234",
//    BLURT_UDP_THREAD_COUNT + "=2",
//    BLURT_UDP_IPV6 + "=false",
//    BLURT_ENABLED + "=true",
//    BLURT_SEND + "=true",
//    BLURT_RECEIVE + "=true"})
@Defaults(value = {BLURT_UDP_HOST + "=ff02::1",
    BLURT_UDP_PORT + "=" + DEFAULT_UDP_PORT,
    BLURT_UDP_THREAD_COUNT + "=6",
    BLURT_UDP_IPV6 + "=true",
    BLURT_ENABLED + "=true",
    BLURT_SEND + "=true",
    BLURT_RECEIVE + "=true"})
@Singleton
class BlurtUDP implements Blurt, BlurtControl {

    static TypeReference MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };
    public static final String DEFAULT_UDP_HOST = "224.0.0.1";
    public static final String DEFAULT_IPV6_UDP_HOST = "ff02::1";
    public static final int DEFAULT_UDP_PORT = 41234;

    static final Logger logger = Logger.getLogger(BlurtUDP.class.getName());

    private final ExecutorService dispatchThreadPool;
    private final ExecutorService serverThreadPool = Executors.newSingleThreadExecutor();
    private final UDPMessageLoop loop;
    private final Queue<Object> outbound = new ConcurrentLinkedQueue<>();
    private final boolean enabled;
    private final boolean allowSend;
    private final ApplicationInfo info;
    private final boolean useHeartbeat;
    private final boolean autoStart;
    private final Timer heartbeat = new Timer(BlurtUDP.class.getName());
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final long heartbeatInterval;
    @Inject
    BlurtUDP(ShutdownHookRegistry hooks, BlurtReceiver receiver, Settings settings, BlurtCodec codec, ApplicationInfo info) throws ClosedChannelException, SocketException, IOException {
        this.info = info;
        int threadCount = settings.getInt(BLURT_UDP_THREAD_COUNT, 2);
        Checks.nonZero(BLURT_UDP_THREAD_COUNT, threadCount);
        Checks.nonNegative(BLURT_UDP_THREAD_COUNT, threadCount);
        boolean ipv6 = settings.getBoolean(BLURT_UDP_IPV6, false);
        enabled = settings.getBoolean(BLURT_ENABLED, true);
        allowSend = settings.getBoolean(BLURT_SEND, true);
        autoStart = settings.getBoolean(BLURT_AUTOSTART, true);
        heartbeatInterval = settings.getLong(BLURT_HEARTBEAT_INTERVAL_MILLIS, DEFAULT_HEARTBEAT_INTERVAL);
        boolean allowReceive = receiver instanceof BlurtReceiver.NoOpBlurtReceiver ? false : settings.getBoolean(BLURT_RECEIVE, true);
        boolean loopbackOnly = settings.getBoolean(BLURT_LOOPBACK_ONLY, false);
        useHeartbeat = heartbeatInterval > 0 && settings.getBoolean(BLURT_HEARTBEAT, true);
        String udpHost = settings.getString(BLURT_UDP_HOST, ipv6 ? DEFAULT_IPV6_UDP_HOST : DEFAULT_UDP_HOST);
        int udpPort = settings.getInt(BLURT_UDP_PORT, 41234);
        Checks.nonZero(BLURT_UDP_PORT, udpPort);
        Checks.nonNegative(BLURT_UDP_PORT, udpPort);
        logger.log(Level.INFO, "Will blurt to {0}:{1}", new Object[]{udpHost, udpPort});
        this.dispatchThreadPool = Executors.newFixedThreadPool(threadCount);
        String ifaceName = settings.getString(BLURT_UDP_NETWORK_INTERFACE);
        if (ifaceName != null && loopbackOnly) {
            logger.log(Level.WARNING, "Ignoring " + BLURT_UDP_NETWORK_INTERFACE
                    + " because " + BLURT_LOOPBACK_ONLY + " is true");
        }
        ProtocolFamily family = ipv6 ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
        InetAddress address;
        InetSocketAddress addr;
        if (family == StandardProtocolFamily.INET) {
            address = InetAddress.getByName(udpHost);
            addr = new InetSocketAddress(address, udpPort);
        } else {
            address = Inet6Address.getByName(udpHost);
            addr = new InetSocketAddress(address, udpPort);
        }

        loop = new UDPMessageLoop(info, findNetworkInterface(ifaceName, loopbackOnly),
                address, addr, outbound, receiver, dispatchThreadPool,
                udpPort, family, codec, allowSend, allowReceive, settings);
        hooks.add(new Stopper());
    }

    private NetworkInterface findNetworkInterface(String optionalName, boolean loopbackOnly) throws SocketException {
        NetworkInterface in = optionalName == null ? null : NetworkInterface.getByName(optionalName);
        if (in == null) {
            for (NetworkInterface i : CollectionUtils.toIterable(NetworkInterface.getNetworkInterfaces())) {
                if (loopbackOnly) {
                    if (i.isLoopback()) {
                        in = i;
                        break;
                    }
                } else {
                    if (i.isLoopback()) {
                        continue;
                    }
                    if (i.isUp()) {
                        in = i;
                        break;
                    }
                }
            }
        }
        if (in == null) {
            throw new Error("No non-loopback network interface is available");
        }
        return in;
    }

    private final class Stopper implements Runnable {

        @Override
        public void run() {
            try {
                stop();
            } catch (InterruptedException | IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void start() throws ClosedChannelException, SocketException, IOException {
        if (!enabled) {
            return;
        }
        if (!started.getAndSet(true)) {
            serverThreadPool.submit(loop);
            if (allowSend && useHeartbeat) {
                heartbeat.scheduleAtFixedRate(new HeartbeatTimerTask(), 10, heartbeatInterval);
            }
        }
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        if (!enabled) {
            return;
        }
        if (!stopped.getAndSet(true)) {
            heartbeat.cancel();
            try {
                loop.stop();
            } finally {
                try {
                    serverThreadPool.shutdown();
                    serverThreadPool.awaitTermination(30, TimeUnit.SECONDS);
                } finally {
                    dispatchThreadPool.shutdown();
                    dispatchThreadPool.awaitTermination(30, TimeUnit.SECONDS);
                }
            }
        }
    }

    @Override
    public boolean blurt(Object message) {
        if (autoStart) {
            if (!started.get()) {
                try {
                    start();
                } catch (SocketException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
        if (!enabled || !allowSend) {
            return false;
        }
        if (!(message instanceof Map)) {
            try {
                ObjectMapper om = new ObjectMapper();
                String s = om.writeValueAsString(message);
                Map m = om.readValue(s, Map.class);
                message = m;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        Map<String, Object> m = (Map<String, Object>) message;
        m.put("i", info.uniqueIdentifier());

        boolean result = outbound.offer(message);
        if (result) {
            loop.touch();
        } else {
            System.out.println("could not enqueue " + message);
        }
        return result;
    }

    @Override
    public boolean blurt(String key, Object value) {
        if (!enabled || !allowSend) {
            return false;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("i", info.uniqueIdentifier());
        m.put(key, value);
        return blurt(m);
    }

    private class HeartbeatTimerTask extends java.util.TimerTask {

        public HeartbeatTimerTask() {
        }
        private final long start = System.currentTimeMillis();

        @Override
        public void run() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uptime", System.currentTimeMillis() - start);
            blurt(m);
        }
    }
}
