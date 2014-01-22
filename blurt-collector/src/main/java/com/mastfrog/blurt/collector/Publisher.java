package com.mastfrog.blurt.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import static com.mastfrog.blurt.collector.CollectorModule.EVENTS;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.util.Exceptions;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;

/**
 *
 * @author Tim Boudreau
 */
@Singleton
public class Publisher {

    private final Set<Channel> channels = Sets.newConcurrentHashSet();
    private final ExecutorService exe = Executors.newFixedThreadPool(1);
    private final LinkedBlockingQueue<DBObject> messages = new LinkedBlockingQueue<>();
    private final DBCollection collection;
    private final ObjectMapper mapper;
    private final MessagePublisher publisher = new MessagePublisher();

    @Inject
    Publisher(@Named(EVENTS) DBCollection collection, ObjectMapper mapper, ShutdownHookRegistry reg) {
        this.collection = collection;
        this.mapper = mapper;
        reg.add(new OnShutdown());
        exe.submit(new MessagePublisher());
    }

    void publish(DBObject message) {
        messages.offer(message);
    }

    void subscribe(Channel channel) {
        if (channel.isOpen()) {
            channels.add(channel);
        }
    }

    private static final String ID_BASE = Long.toString(System.currentTimeMillis(), 36) + "_";

    private class MessagePublisher implements Runnable {

        boolean done;
        private long idx = 0;

        private final boolean done() {
            final boolean result = done || Thread.interrupted();
            System.out.println("Done loop? " + result);
            return result;
        }

        @Override
        public void run() {
            try {
                for (;;) {
                    try {
                        DBObject msg = messages.take();
                        if (done()) {
                            return;
                        }
                        WriteResult res = collection.insert(msg, WriteConcern.FSYNCED);
                        System.out.println("RESULT " + res + " for \n" + mapper.writeValueAsString(msg));

                        Iterator<Channel> it = channels.iterator();
//                        if (!it.hasNext()) {
//                            continue;
//                        }
                        String id = msg.get("_id") + "";
                        if (id.isEmpty()) {
                            id = ID_BASE + idx++;
                        }
                        msg.removeField("_id");
                        byte[] bytes = new StringBuilder("id: ")
                                        .append(id)
                                        .append("\n").append("data: ")
                                        .append(mapper.writeValueAsString(msg))
                                        .append("\n\n").toString().getBytes(CharsetUtil.UTF_8);
                        try {
                            for (; it.hasNext();) {
                                ByteBuf message = Unpooled.wrappedBuffer(bytes);
                                if (done()) {
                                    return;
                                }
                                Channel c = it.next();
                                System.out.println("Channel is open " + c.isOpen() + " active " + c.isActive() + " writable " + c.isWritable());
                                if (!c.isOpen()) {
                                    it.remove();
                                    continue;
                                }

                                c.writeAndFlush(message.duplicate());
                                message.resetReaderIndex();
                                message.resetWriterIndex();
                                if (done()) {
                                    return;
                                }
                            }
                        } finally {
//                            message.release();
                        }
                    } catch (InterruptedException ex) {
                        if (done()) {
                            return;
                        }
                        Exceptions.printStackTrace(ex);
                    } catch (JsonProcessingException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                System.out.println("Exit Loop");
            }
        }
    }

    private class OnShutdown implements Runnable {

        @Override
        public void run() {
            publisher.done = true;
            exe.shutdown();
            try {
                exe.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            for (Channel channel : channels) {
                try {
                    channel.close();
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
}
