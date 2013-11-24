package com.mastfrog.blurt;

import com.google.inject.Inject;
import com.mastfrog.util.ConfigurationError;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;

/**
 *
 * @author Tim Boudreau
 */
class DefaultBlurtControl implements BlurtControl {
    private final BlurtControl delegate;
    @Inject
    DefaultBlurtControl(Blurt blurt) {
        if (!(blurt instanceof BlurtControl)) {
            throw new ConfigurationError(blurt + " does not implement BlurtControl - cannot use it");
        }
        delegate = (BlurtControl) blurt;
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        delegate.stop();
    }

    @Override
    public void start() throws ClosedChannelException, SocketException, IOException {
        delegate.start();
    }
}
