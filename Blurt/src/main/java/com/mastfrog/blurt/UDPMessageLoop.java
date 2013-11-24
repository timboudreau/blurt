package com.mastfrog.blurt;

import static com.mastfrog.blurt.BlurtUDP.BLURT_UDP_BROADCAST;
import static com.mastfrog.blurt.BlurtUDP.BLURT_UDP_MULTICAST_LOOPBACK;
import static com.mastfrog.blurt.BlurtUDP.BLURT_UDP_MULTICAST_TTL;
import static com.mastfrog.blurt.BlurtUDP.BLURT_UDP_TRAFFIC_CLASS;
import com.mastfrog.cluster.ApplicationInfo;
import com.mastfrog.settings.Settings;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

/**
 * Inner loop which receives messages
 *
 * @author Tim Boudreau
 */
final class UDPMessageLoop implements Runnable {

    private final InetSocketAddress address;
    private final Queue<Object> outbound;
    final BlurtReceiver receiver;
    private final Selector selector;
    private final MembershipKey membershipKey;
    private final SelectionKey receiveKey;
    private final SelectionKey sendKey;
    private final DatagramChannel readChannel;
    private final ExecutorService dispatchThreadPool;
    volatile boolean stopped;
    private final BlurtCodec codec;
    private final int bufferSize;
    private static final int DEFAULT_UDP_BUFFER_SIZE = 1024;
    private final ApplicationInfo info;

    UDPMessageLoop(ApplicationInfo info, final NetworkInterface interf, final InetAddress group, InetSocketAddress addr, final Queue<Object> outbound, final BlurtReceiver receiver, final ExecutorService dispatchThreadPool, final int udpPort, ProtocolFamily family, BlurtCodec codec, boolean allowSend, boolean allowReceive, Settings settings) throws ClosedChannelException, SocketException, IOException {
        this.dispatchThreadPool = dispatchThreadPool;
        this.address = addr;
        this.outbound = outbound;
        this.receiver = receiver;
        this.info = info;
        this.codec = codec;
        SocketAddress bindTo = family == StandardProtocolFamily.INET6 ? new InetSocketAddress(udpPort) : addr;
        bufferSize = settings.getInt(Blurt.BLURT_UDP_BUFFER_SIZE, DEFAULT_UDP_BUFFER_SIZE);
        boolean multicastLoopback = settings.getBoolean(BLURT_UDP_MULTICAST_LOOPBACK, true);
        boolean broadcast = settings.getBoolean(BLURT_UDP_BROADCAST, true);
        int ttl = settings.getInt(BLURT_UDP_MULTICAST_TTL, 2);
        int trafficClass = settings.getInt(BLURT_UDP_TRAFFIC_CLASS, 0x04);
        readChannel = DatagramChannel.open(family);
        readChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true).setOption(StandardSocketOptions.IP_MULTICAST_LOOP, multicastLoopback).setOption(StandardSocketOptions.IP_MULTICAST_IF, interf).setOption(StandardSocketOptions.SO_BROADCAST, broadcast).bind(bindTo).setOption(StandardSocketOptions.IP_MULTICAST_TTL, ttl).socket().setTrafficClass(trafficClass);
        readChannel.configureBlocking(false);
        membershipKey = readChannel.join(group, interf);
        selector = Selector.open();
        receiveKey = readChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, "recv");
        if (allowReceive) {
            receiveKey.interestOps(SelectionKey.OP_READ);
            readChannel.register(selector, SelectionKey.OP_READ);
        }
        DatagramChannel writeChannel = DatagramChannel.open(family).setOption(StandardSocketOptions.SO_REUSEADDR, true).setOption(StandardSocketOptions.IP_MULTICAST_LOOP, multicastLoopback).setOption(StandardSocketOptions.IP_MULTICAST_IF, interf).setOption(StandardSocketOptions.SO_BROADCAST, broadcast).setOption(StandardSocketOptions.IP_MULTICAST_TTL, ttl);
        writeChannel.socket().setTrafficClass(trafficClass);
        writeChannel.configureBlocking(false);
        sendKey = writeChannel.register(selector, SelectionKey.OP_WRITE, "send");
        if (allowSend) {
            sendKey.interestOps(SelectionKey.OP_WRITE);
            writeChannel.register(selector, SelectionKey.OP_WRITE);
        }
    }

    void stop() {
        stopped = true;
        membershipKey.drop();
        receiveKey.cancel();
        sendKey.cancel();
        try {
            selector.close();
        } catch (IOException ex) {
            BlurtUDP.logger.log(Level.SEVERE, null, ex);
        } finally {
            try {
                readChannel.close();
            } catch (IOException ex) {
                BlurtUDP.logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    private class Sender implements Runnable {

        private final Message<Map<String, Object>> payload;

        public Sender(Message<Map<String, Object>> payload) {
            this.payload = payload;
        }

        @Override
        public void run() {
            receiver.receive(payload);
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Blurt UDP Socket Loop");
        byte[] zeros = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.limit(bufferSize);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (;;) {
            if (stopped) {
                return;
            }
            try {
                selector.select();
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext();) {
                    if (stopped) {
                        return;
                    }
                    SelectionKey key = it.next();
                    if (key.isReadable()) {
                        DatagramChannel ch = (DatagramChannel) key.channel();
                        buffer.clear();
                        buffer.put(zeros);
                        buffer.clear();
                        SocketAddress a = ch.receive(buffer);
                        int byteCount = buffer.position();
                        BlurtUDP.logger.log(Level.FINEST, "BlurtUDP read {0} bytes", byteCount);
                        if (byteCount > 0) {
                            buffer.flip();
                            try {
                                try {
                                    final Map<String, Object> m = new LinkedHashMap<>(codec.decode(buffer, Map.class));
                                    String id = (String) m.get("i");
                                    if (id != null && info.uniqueIdentifier().equals(id)) {
                                        continue;
                                    }
                                    m.remove("i");
                                    dispatchThreadPool.submit(new Sender(new Message(a, m, id == null ? info.uniqueIdentifier() : id)));
                                } catch (IOException ioe) {
                                    if (ioe.getMessage().contains("no single-String constructor")) {
                                        final Map<String, Object> m = new HashMap<>();
                                        String id = (String) m.get("i");
                                        m.remove("i");
                                        buffer.position(0);
                                        m.put("payload", codec.decode(buffer, String.class));
                                        dispatchThreadPool.submit(new Sender(new Message<>(a, m, id != null ? id : info.uniqueIdentifier())));
                                    }
                                    ioe.printStackTrace();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                buffer.clear();
                                buffer.put(zeros);
                                buffer.clear();
                            }
                        }
                        //If we don't remove it, we'll keep looping over the same packet
                        it.remove();
                    }
                    if (key.isWritable()) {
                        Object obj = outbound.poll();
                        if (obj != null) {
                            DatagramChannel ch = (DatagramChannel) key.channel();
                            buffer.clear();
                            try {
                                codec.encode(obj, buffer);
                                buffer.flip();
                                int bytes = ch.send(buffer, address);

                                if (bytes == 0) { // Underlying buffer blocked
                                    outbound.offer(obj);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            buffer.clear();
                        }
                        if (!outbound.isEmpty()) {
                            sendKey.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                // select() returns immediately
                // We do want some throttling here
                Thread.sleep(200);
            } catch (Exception ex) {
                if (stopped) {
                    return;
                }
            }
        }
    }

    void touch() {
        sendKey.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }
}
