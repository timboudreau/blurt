package com.mastfrog.blurt;

import com.mastfrog.blurt.BlurtReceiver.NoOpBlurtReceiver;
import com.google.inject.ImplementedBy;
import java.util.Map;

/**
 * A thing which receives blurts.
 *
 * @author Tim Boudreau
 */
@ImplementedBy(NoOpBlurtReceiver.class)
public abstract class BlurtReceiver {

    protected abstract void receive(Message<Map<String,Object>> map);

    static final class NoOpBlurtReceiver extends BlurtReceiver {
        @Override
        protected void receive(Message<Map<String,Object>> map) {
            //do nothing
        }
    }
}
