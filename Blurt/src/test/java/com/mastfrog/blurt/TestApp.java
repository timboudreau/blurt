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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import static com.mastfrog.blurt.Blurt.BLURT_HEARTBEAT;
import static com.mastfrog.blurt.Blurt.BLURT_RECEIVE;
import static com.mastfrog.blurt.Blurt.BLURT_SEND;
import static com.mastfrog.blurt.Blurt.BLURT_UDP_HOST;
import static com.mastfrog.blurt.Blurt.BLURT_UDP_IPV6;
import static com.mastfrog.blurt.Blurt.BLURT_UDP_PORT;
import static com.mastfrog.blurt.BlurtUDP.DEFAULT_UDP_HOST;
import static com.mastfrog.blurt.BlurtUDP.logger;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.settings.MutableSettings;
import com.mastfrog.settings.SettingsBuilder;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

/**
 *
 * @author tim
 */
public class TestApp {
    
    public static void main(final String[] args) {
        class M extends AbstractModule implements Runnable {

            private final ByteBuffer newline = ByteBuffer.wrap(new byte[]{'\n'});
            private final ObjectMapper m = new ObjectMapper();
            private final FileChannel ch;

            M() throws IOException {
                if (args.length > 0) {
                    File f = new File(args[0]);
                    RandomAccessFile raf = new RandomAccessFile(f, "rw");
                    ch = raf.getChannel();
                    ch.position(ch.size());
                } else {
                    ch = null;
                }
            }

            @Override
            protected void configure() {
                bind(BlurtCodec.class).to(BsonCodec.class);
                bind(BlurtReceiver.class).toInstance(new BlurtReceiver() {
                    @Override
                    public void receive(Message<Map<String, Object>> object) {
                        System.out.println("Message from " + object.getInfo().applicationName() + " " + object.getInfo().installationIdentifier() + " " + object.getInfo().processIdentifier());
                        System.out.println(object);
                        if (ch != null && ch.isOpen()) {
                            try {
                                String s = m.writeValueAsString(object);
                                ch.write(ByteBuffer.wrap(s.getBytes(Charset.forName("UTF-8"))));
                                ch.write(newline);
                            } catch (IOException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
            }

            @Override
            public void run() {
                if (ch != null) {
                    try {
                        ch.force(true);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            ch.close();
                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        try {
            if (args.length > 0) {
                File f = new File(args[0]);
                System.err.println("Will log to " + f.getAbsolutePath());
                if (!f.exists()) {
                    if (!f.createNewFile()) {
                        System.err.println("Could not create " + f.getAbsolutePath());
                        System.exit(1);
                    }
                }
            }
            M m = new M();
            MutableSettings s = SettingsBuilder.createDefault().buildMutableSettings();
            s.setBoolean(BLURT_SEND, true);
            s.setBoolean(BLURT_RECEIVE, true);
            s.setBoolean(BLURT_HEARTBEAT, false);
            s.setInt(BLURT_UDP_PORT, 41234);
            s.setBoolean(BLURT_UDP_IPV6, false);
            s.setString(BLURT_UDP_HOST, DEFAULT_UDP_HOST);
            Dependencies deps = new Dependencies(s, m);
            if (m.ch != null) {
                ShutdownHookRegistry reg = deps.getInstance(ShutdownHookRegistry.class);
                reg.add(m);
            }
            final BlurtUDP blurt = deps.getInstance(BlurtUDP.class);
            blurt.start();
            Timer t = new Timer("Hoo");
            t.scheduleAtFixedRate(new TimerTask() {
                int ix = 0;

                @Override
                public void run() {
                    Map<String, Object> m = new HashMap<>();
                    m.put("ix", ix++);
                    m.put("hey", "hoo");
                    blurt.blurt(m);
                    System.out.println("Send " + m);
                }
            }, 5000, 10000);
            Thread.sleep(20000);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
