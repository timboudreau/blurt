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
import static com.mastfrog.blurt.BlurtUDP.*;
import com.google.inject.Binder;
import com.google.inject.Module;
import static com.mastfrog.blurt.Blurt.BLURT_HEARTBEAT;
import com.mastfrog.cluster.ApplicationInfo;
import static com.mastfrog.cluster.ApplicationInfo.SYSTEM_PROPERTY_NO_WRITES;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.settings.MutableSettings;
import com.mastfrog.settings.SettingsBuilder;
import com.mastfrog.util.strings.RandomStrings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.junit.Test;

public class BlurtTest extends TestCase {

    private static final int COUNT = 15;
    
    static {
        System.setProperty(SYSTEM_PROPERTY_NO_WRITES, "true");
    }

    private String randomIp() {
        Random r = new Random(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder("224.");
        for (int i = 0; i < 3; i++) {
            sb.append(r.nextInt(255));
            if (i != 2) {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    @Test
    public void test() throws Exception {
//        if (true) {
//            //Pending - figure out why this fails on VMs
//            return;
//        }
        
        MutableSettings sa = SettingsBuilder.createDefault().buildMutableSettings();
        String ip = randomIp();
        sa.setBoolean(BLURT_LOOPBACK_ONLY, true);
        sa.setString(BLURT_UDP_HOST, ip);
        sa.setString(BLURT_UDP_PORT, "59215");
        sa.setString(BLURT_NAME, getClass().getSimpleName());

        sa.setBoolean(BLURT_UDP_MULTICAST_LOOPBACK, true);
        sa.setBoolean(BLURT_UDP_BROADCAST, false);
        sa.setInt(BLURT_UDP_MULTICAST_TTL, 2);
        sa.setBoolean(BLURT_RECEIVE, true);
        sa.setBoolean(BLURT_SEND, true);
        sa.setBoolean(BLURT_HEARTBEAT, false);

        MutableSettings sb = new SettingsBuilder().add(sa).buildMutableSettings();

        sb.setBoolean(BLURT_SEND, false);

        CountDownLatch latch = new CountDownLatch(COUNT - 5);
        Br br = new Br(latch);
        Dependencies da = new Dependencies(sa);
        Dependencies db = new Dependencies(sb, br);

        BlurtControl sctl = da.getInstance(BlurtControl.class);
        BlurtControl rctl = db.getInstance(BlurtControl.class);
        Blurt sender = da.getInstance(Blurt.class);
        Blurt receiver = db.getInstance(Blurt.class);
        
        sctl.start();
        rctl.start();
        try {
            Set<Thing> things = new LinkedHashSet<>();
            for (int i = 0; i < COUNT; i++) {
                Thing t = new Thing("Thing " + i, (i % 2) == 0);
                System.out.println("Send " + t);
                things.add(t);
                sender.blurt(t);
            }

            latch.await(30, TimeUnit.SECONDS);
            Thread.sleep(2000);

            // Make sure something got there
            assertNotSame("Nothing got through", 0, br.things.size());
            // This is UDP - packets are not guaranteed delivery, so check that
            // what we got is what we sent
            
            assertEquals(things.size(), br.things.size());

            // NOTE: This test could fail spuriously if two versions are run
            // simultaneously on the same network - we try to avoid that with
            // randomizing the IP.  Hmm, maybe we should tie it to the loopback interface?
            assertTrue("Non match " + br.things + " \n versus \n" + things,things.containsAll(br.things));
        } finally {
            da.shutdown();
            db.shutdown();
        }
    }

    static class Br extends BlurtReceiver implements Module, ApplicationInfo {

        private final CountDownLatch latch;

        Br(CountDownLatch latch) {
            this.latch = latch;
        }
        List<Exception> exs = new ArrayList<>();
        Set<Thing> things = Collections.synchronizedSet(new LinkedHashSet<Thing>());

        @Override
        public synchronized void receive(Message<Map<String,Object>> msg) {
            System.out.println("RECEIVE " + msg);
            try {
                String json = new ObjectMapper().writeValueAsString(msg.message());
                Thing th = new ObjectMapper().readValue(json, Thing.class);
                System.out.println("Receive t " + th);
                things.add(th);
                latch.countDown();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }

        @Override
        public void configure(Binder binder) {
            binder.bind(BlurtReceiver.class).toInstance(this);
            binder.bind(ApplicationInfo.class).toInstance(this);
        }

        @Override
        public String applicationName() {
            return "test";
        }

        @Override
        public String installationIdentifier() {
            return "one";
        }

        @Override
        public String processIdentifier() {
            return new RandomStrings().get(7);
        }

        @Override
        public String uniqueIdentifier() {
            return installationIdentifier() + ':' + processIdentifier() + ':' + applicationName();
        }
    }

    public static class Thing {
        static Random r = new Random(System.currentTimeMillis());
        public String foo;
        public long uptime;
        public boolean bar;
        public int rand;
        public Map<String, Object> from;

        public Thing() {
        }

        public Thing(String foo, boolean bar) {
            this.foo = foo;
            this.bar = bar;
            this.rand = r.nextInt();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + Objects.hashCode(this.foo);
            hash = 31 * hash + (this.bar ? 1 : 0);
            hash = 31 * hash + this.rand;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Thing other = (Thing) obj;
            if (!Objects.equals(this.foo, other.foo)) {
                return false;
            }
            if (this.bar != other.bar) {
                return false;
            }
            if (this.rand != other.rand) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Thing{" + "foo=" + foo  + ", bar=" + bar + ", rand=" + rand + '}';
        }
    }
}
